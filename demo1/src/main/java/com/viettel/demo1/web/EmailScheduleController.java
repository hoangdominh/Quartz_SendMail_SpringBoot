package com.viettel.demo1.web;

import com.viettel.demo1.payload.EmailRequest;
import com.viettel.demo1.payload.EmailResponse;
import com.viettel.demo1.quartz.job.EmailJob;
import lombok.extern.slf4j.Slf4j;
import org.quartz.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.UUID;

@Slf4j
@RestController
public class EmailScheduleController {
    @Autowired
    private Scheduler scheduler;

    @PostMapping("/schedule/email")
    public ResponseEntity<EmailResponse> scheduleEmail(@RequestBody EmailRequest emailRequest){
        try {
            ZonedDateTime dateTime = ZonedDateTime.of(emailRequest.getDateTime(),emailRequest.getTimeZone());
            if(dateTime.isBefore(ZonedDateTime.now())){
                EmailResponse emailResponse = new EmailResponse(false,
                        "dateTime must be after current time.");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(emailResponse);
            }
            JobDetail jobDetail = buildJobDetail(emailRequest);
            Trigger trigger = buildTrigger(jobDetail, dateTime);
            scheduler.scheduleJob(jobDetail, trigger);
            EmailResponse emailResponse = new EmailResponse(true,
                    jobDetail.getKey().getName(), jobDetail.getKey().getGroup(),
                    "Email Scheduled Successful!");
            log.info("Email Scheduled Successful!" + LocalDateTime.now());
            return ResponseEntity.ok(emailResponse);
        }catch (SchedulerException exception){
            log.error("Error while scheduling email", exception);
            EmailResponse emailResponse = new EmailResponse(false, "Error while scheduling email. Please try again later!");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(emailResponse);
        }
    }


    // X??y d???ng JobDetails
    /**
     JobDetail ?????i di???n cho m???t phi??n b???n c???a Job. N?? ch???a d??? li???u b??? sung d?????i d???ng JobDataMap
     ???????c chuy???n t???i Job khi n?? ???????c th???c thi

     M???t JobDetail ???????c x??c ?????nh b???i JobKey bao g???m  name(t??n) v?? group(nh??m). T??n ph???i l?? duy nh???t trong m???t nh??m

     */
    private JobDetail buildJobDetail(EmailRequest scheduleEmailRequest){
        JobDataMap jobDataMap = new JobDataMap();

        jobDataMap.put("email", scheduleEmailRequest.getEmail());
        jobDataMap.put("subject", scheduleEmailRequest.getSubject());
        jobDataMap.put("body", scheduleEmailRequest.getBody());

        return JobBuilder.newJob(EmailJob.class)
                .withIdentity(UUID.randomUUID().toString(),"email-jobs")
                .withDescription("Send Email Job")
                .usingJobData(jobDataMap)
                .storeDurably()
                .build();
    }



    // X??y d???ng Trigger
    /**
     Trigger, nh?? t??n cho th???y, x??c ?????nh l???ch tr??nh m?? t???i ???? m???t C??ng vi???c nh???t ?????nh s??? ???????c th???c hi???n.
     M???t C??ng vi???c c?? th??? c?? nhi???u Tr??nh k??ch ho???t, nh??ng m???t Tr??nh k??ch ho???t ch??? c?? th??? ???????c li??n k???t v???i m???t C??ng vi???c

     M???i Trigger ???????c x??c ?????nh b???i m???t TriggerKey bao g???m m???t t??n v?? m???t nh??m. T??n ph???i l?? duy nh???t trong m???t nh??m.

     C??ng gi???ng nh?? JobDetails, Trigger c??ng c?? th??? g???i c??c tham s??? / d??? li???u ?????n Job
     */
    private Trigger buildTrigger(JobDetail jobDetail, ZonedDateTime startAt){
        /**
         TriggerBuilder ???????c s??? d???ng ????? kh???i t???o c??c Trigger.
         */
        return TriggerBuilder.newTrigger()
                .forJob(jobDetail)
                .withIdentity(jobDetail.getKey().getName(),"email-triggers")
                .withDescription("Send Email Trigger")
                .startAt(Date.from(startAt.toInstant()))
                .withSchedule(SimpleScheduleBuilder.simpleSchedule().withMisfireHandlingInstructionFireNow())
                .build();
    }

    @GetMapping("get")
    public ResponseEntity<String> getApiTest(){
        return ResponseEntity.ok("Get API Test - Pass");
    }

}
