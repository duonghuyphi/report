package com.engagePlus.report.repository;

import com.engagePlus.report.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
}
