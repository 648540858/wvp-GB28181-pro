/*
* 20250519
*/
DELIMITER //  -- 重定义分隔符避免分号冲突
CREATE PROCEDURE `wvp_20250519`()
BEGIN
    IF NOT EXISTS (SELECT column_name FROM information_schema.columns
                   WHERE TABLE_SCHEMA = (SELECT DATABASE()) and  table_name = 'wvp_device' and column_name = 'device_type')
    THEN
        ALTER TABLE wvp_device ADD device_type  integer;
    END IF;
END; //
call wvp_20250519();
DROP PROCEDURE wvp_20250519;
DELIMITER ;



