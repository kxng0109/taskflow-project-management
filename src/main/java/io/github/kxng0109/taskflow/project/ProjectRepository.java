package io.github.kxng0109.taskflow.project;

import io.github.kxng0109.taskflow.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProjectRepository extends JpaRepository<Project,Long> {

    List<Project> findByMembersContaining(User currentUser);
}
