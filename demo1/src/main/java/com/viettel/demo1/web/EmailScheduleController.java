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


    // Xây dựng JobDetails
    /**
     JobDetail đại diện cho một phiên bản của Job. Nó chứa dữ liệu bổ sung dưới dạng JobDataMap
     được chuyển tới Job khi nó được thực thi

     Một JobDetail được xác định bởi JobKey bao gồm  name(tên) và group(nhóm). Tên phải là duy nhất trong một nhóm

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



    // Xây dựng Trigger
    /**
     Trigger, như tên cho thấy, xác định lịch trình mà tại đó một Công việc nhất định sẽ được thực hiện.
     Một Công việc có thể có nhiều Trình kích hoạt, nhưng một Trình kích hoạt chỉ có thể được liên kết với một Công việc

     Mỗi Trigger được xác định bởi một TriggerKey bao gồm một tên và một nhóm. Tên phải là duy nhất trong một nhóm.

     Cũng giống như JobDetails, Trigger cũng có thể gửi các tham số / dữ liệu đến Job
     */
    private Trigger buildTrigger(JobDetail jobDetail, ZonedDateTime startAt){
        /**
         TriggerBuilder được sử dụng để khởi tạo các Trigger.
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
