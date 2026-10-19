/*
* 20260606 添加用户默认密码标记字段（默认密码强制修改）
*/
DROP PROCEDURE IF EXISTS `wvp_20260606`;
DELIMITER //  -- 重定义分隔符避免分号冲突
CREATE PROCEDURE `wvp_20260606`()
BEGIN
    IF NOT EXISTS (SELECT column_name FROM information_schema.columns
                   WHERE TABLE_SCHEMA = (SELECT DATABASE()) and  table_name = 'wvp_user' and column_name = 'default_password')
    THEN
        ALTER TABLE wvp_user ADD default_password bool default false COMMENT '是否使用默认密码';
    END IF;

    -- 仅当 admin 的密码仍是默认密码时才标记 default_password=TRUE，已改过密码的不强制
    UPDATE wvp_user
       SET default_password = TRUE
     WHERE username = 'admin'
       AND password = '21232f297a57a5a743894a0e4a801fc3';
END; //
DELIMITER ;
call wvp_20260606();
DROP PROCEDURE wvp_20260606;

/*
* 20261019 录像计划条目表字段改名，避免使用数据库关键字 start / stop
*/
DROP PROCEDURE IF EXISTS `wvp_20261019`;
DELIMITER //  -- 重定义分隔符避免分号冲突
CREATE PROCEDURE `wvp_20261019`()
BEGIN
    IF EXISTS (SELECT column_name FROM information_schema.columns
                   WHERE TABLE_SCHEMA = (SELECT DATABASE()) and  table_name = 'wvp_record_plan_item' and column_name = 'start')
       AND NOT EXISTS (SELECT column_name FROM information_schema.columns
                   WHERE TABLE_SCHEMA = (SELECT DATABASE()) and  table_name = 'wvp_record_plan_item' and column_name = 'start_time')
    THEN
        ALTER TABLE wvp_record_plan_item CHANGE `start` start_time int COMMENT '开始时间（分钟）';
    END IF;

    IF EXISTS (SELECT column_name FROM information_schema.columns
                   WHERE TABLE_SCHEMA = (SELECT DATABASE()) and  table_name = 'wvp_record_plan_item' and column_name = 'stop')
       AND NOT EXISTS (SELECT column_name FROM information_schema.columns
                   WHERE TABLE_SCHEMA = (SELECT DATABASE()) and  table_name = 'wvp_record_plan_item' and column_name = 'stop_time')
    THEN
        ALTER TABLE wvp_record_plan_item CHANGE `stop` stop_time int COMMENT '结束时间（分钟）';
    END IF;
END; //
DELIMITER ;
call wvp_20261019();
DROP PROCEDURE wvp_20261019;
