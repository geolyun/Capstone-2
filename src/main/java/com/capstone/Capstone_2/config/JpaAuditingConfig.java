package com.capstone.Capstone_2.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

// BaseTimeEntity.java에서 @CreatedDate, @LastModifiedDate 등의 JPA Auditing 어노테이션을 사용
@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {
}
