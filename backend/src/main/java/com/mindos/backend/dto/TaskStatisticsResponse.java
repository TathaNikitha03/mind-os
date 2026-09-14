package com.mindos.backend.dto;

import java.util.List;
import java.util.Map;

public class TaskStatisticsResponse {

    private long totalTasks;
    private long completedTasks;
    private long inProgressTasks;
    private long todoTasks;
    private long overdueTasks;
    private long todayTasks;
    private long blockedTasks;
    private double completionPercentage;
    private Map<String, Long> priorityBreakdown;
    private Map<String, Long> categoryBreakdown;
    private List<TaskResponse> todayTasksList;
    private List<TaskResponse> upcomingTasksList;
    private List<TaskResponse> overdueTasksList;
    private List<TaskResponse> blockedTasksList;

    public TaskStatisticsResponse() {}

    public static TaskStatisticsResponseBuilder builder() {
        return new TaskStatisticsResponseBuilder();
    }

    public static class TaskStatisticsResponseBuilder {
        private long totalTasks;
        private long completedTasks;
        private long inProgressTasks;
        private long todoTasks;
        private long overdueTasks;
        private long todayTasks;
        private long blockedTasks;
        private double completionPercentage;
        private Map<String, Long> priorityBreakdown;
        private Map<String, Long> categoryBreakdown;
        private List<TaskResponse> todayTasksList;
        private List<TaskResponse> upcomingTasksList;
        private List<TaskResponse> overdueTasksList;
        private List<TaskResponse> blockedTasksList;

        public TaskStatisticsResponseBuilder totalTasks(long totalTasks) { this.totalTasks = totalTasks; return this; }
        public TaskStatisticsResponseBuilder completedTasks(long completedTasks) { this.completedTasks = completedTasks; return this; }
        public TaskStatisticsResponseBuilder inProgressTasks(long inProgressTasks) { this.inProgressTasks = inProgressTasks; return this; }
        public TaskStatisticsResponseBuilder todoTasks(long todoTasks) { this.todoTasks = todoTasks; return this; }
        public TaskStatisticsResponseBuilder overdueTasks(long overdueTasks) { this.overdueTasks = overdueTasks; return this; }
        public TaskStatisticsResponseBuilder todayTasks(long todayTasks) { this.todayTasks = todayTasks; return this; }
        public TaskStatisticsResponseBuilder blockedTasks(long blockedTasks) { this.blockedTasks = blockedTasks; return this; }
        public TaskStatisticsResponseBuilder completionPercentage(double completionPercentage) { this.completionPercentage = completionPercentage; return this; }
        public TaskStatisticsResponseBuilder priorityBreakdown(Map<String, Long> priorityBreakdown) { this.priorityBreakdown = priorityBreakdown; return this; }
        public TaskStatisticsResponseBuilder categoryBreakdown(Map<String, Long> categoryBreakdown) { this.categoryBreakdown = categoryBreakdown; return this; }
        public TaskStatisticsResponseBuilder todayTasksList(List<TaskResponse> todayTasksList) { this.todayTasksList = todayTasksList; return this; }
        public TaskStatisticsResponseBuilder upcomingTasksList(List<TaskResponse> upcomingTasksList) { this.upcomingTasksList = upcomingTasksList; return this; }
        public TaskStatisticsResponseBuilder overdueTasksList(List<TaskResponse> overdueTasksList) { this.overdueTasksList = overdueTasksList; return this; }
        public TaskStatisticsResponseBuilder blockedTasksList(List<TaskResponse> blockedTasksList) { this.blockedTasksList = blockedTasksList; return this; }

        public TaskStatisticsResponse build() {
            TaskStatisticsResponse r = new TaskStatisticsResponse();
            r.setTotalTasks(totalTasks);
            r.setCompletedTasks(completedTasks);
            r.setInProgressTasks(inProgressTasks);
            r.setTodoTasks(todoTasks);
            r.setOverdueTasks(overdueTasks);
            r.setTodayTasks(todayTasks);
            r.setBlockedTasks(blockedTasks);
            r.setCompletionPercentage(completionPercentage);
            r.setPriorityBreakdown(priorityBreakdown);
            r.setCategoryBreakdown(categoryBreakdown);
            r.setTodayTasksList(todayTasksList);
            r.setUpcomingTasksList(upcomingTasksList);
            r.setOverdueTasksList(overdueTasksList);
            r.setBlockedTasksList(blockedTasksList);
            return r;
        }
    }

    public long getTotalTasks() { return totalTasks; }
    public void setTotalTasks(long totalTasks) { this.totalTasks = totalTasks; }

    public long getCompletedTasks() { return completedTasks; }
    public void setCompletedTasks(long completedTasks) { this.completedTasks = completedTasks; }

    public long getInProgressTasks() { return inProgressTasks; }
    public void setInProgressTasks(long inProgressTasks) { this.inProgressTasks = inProgressTasks; }

    public long getTodoTasks() { return todoTasks; }
    public void setTodoTasks(long todoTasks) { this.todoTasks = todoTasks; }

    public long getOverdueTasks() { return overdueTasks; }
    public void setOverdueTasks(long overdueTasks) { this.overdueTasks = overdueTasks; }

    public long getTodayTasks() { return todayTasks; }
    public void setTodayTasks(long todayTasks) { this.todayTasks = todayTasks; }

    public long getBlockedTasks() { return blockedTasks; }
    public void setBlockedTasks(long blockedTasks) { this.blockedTasks = blockedTasks; }

    public double getCompletionPercentage() { return completionPercentage; }
    public void setCompletionPercentage(double completionPercentage) { this.completionPercentage = completionPercentage; }

    public Map<String, Long> getPriorityBreakdown() { return priorityBreakdown; }
    public void setPriorityBreakdown(Map<String, Long> priorityBreakdown) { this.priorityBreakdown = priorityBreakdown; }

    public Map<String, Long> getCategoryBreakdown() { return categoryBreakdown; }
    public void setCategoryBreakdown(Map<String, Long> categoryBreakdown) { this.categoryBreakdown = categoryBreakdown; }

    public List<TaskResponse> getTodayTasksList() { return todayTasksList; }
    public void setTodayTasksList(List<TaskResponse> todayTasksList) { this.todayTasksList = todayTasksList; }

    public List<TaskResponse> getUpcomingTasksList() { return upcomingTasksList; }
    public void setUpcomingTasksList(List<TaskResponse> upcomingTasksList) { this.upcomingTasksList = upcomingTasksList; }

    public List<TaskResponse> getOverdueTasksList() { return overdueTasksList; }
    public void setOverdueTasksList(List<TaskResponse> overdueTasksList) { this.overdueTasksList = overdueTasksList; }

    public List<TaskResponse> getBlockedTasksList() { return blockedTasksList; }
    public void setBlockedTasksList(List<TaskResponse> blockedTasksList) { this.blockedTasksList = blockedTasksList; }
}
