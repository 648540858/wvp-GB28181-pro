alter table device
    add subscribeCycleForMobilePosition int null;

alter table device
    add mobilePositionSubmissionInterval int default 5 null;

alter table device
    add subscribeCycleForAlarm int null;