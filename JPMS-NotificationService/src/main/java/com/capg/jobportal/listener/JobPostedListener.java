package com.capg.jobportal.listener;

import java.util.List;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.capg.jobportal.client.AuthServiceClient;
import com.capg.jobportal.dto.UserInfoResponse;
import com.capg.jobportal.event.JobPostedEvent;
import com.capg.jobportal.service.EmailService;

@Component
public class JobPostedListener {

    @Autowired
    private EmailService emailService;

    @Autowired
    private AuthServiceClient authServiceClient;   // inject Feign client

    @RabbitListener(queues = "${rabbitmq.queue}")
    public void handleJobPosted(JobPostedEvent event) {
        System.out.println("Received job posted event: " + event.getTitle());

        try {
            // 1. Send confirmation email to the recruiter who posted the job
            if (event.getRecruiterId() != null) {
                try {
                    UserInfoResponse recruiter = authServiceClient.getUserInfo(event.getRecruiterId());
                    emailService.sendJobPostedConfirmation(recruiter.getEmail(), event);
                    System.out.println("Confirmation email sent to recruiter: " + recruiter.getEmail());
                } catch (Exception e) {
                    System.err.println("Failed to send confirmation email to recruiter: " + e.getMessage());
                }
            }

            // 2. Send job alert emails to all active job seekers
            List<String> emails = authServiceClient.getJobSeekerEmails();

            if (emails != null && !emails.isEmpty()) {
                System.out.println("Sending email to " + emails.size() + " job seekers");
                for (String email : emails) {
                    try {
                        emailService.sendJobAlert(email, event);
                        System.out.println("Email sent to: " + email);
                    } catch (Exception e) {
                        System.err.println("Failed to send email to "
                                + email + ": " + e.getMessage());
                    }
                }
            }

        } catch (Exception e) {
            System.err.println("Error processing job posted event: "
                    + e.getMessage());
        }
    }
}