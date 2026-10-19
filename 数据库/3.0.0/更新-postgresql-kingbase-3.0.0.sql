/*
* 20260606 添加用户默认密码标记字段（默认密码强制修改）
*/
ALTER TABLE wvp_user ADD COLUMN IF NOT EXISTS default_password bool default false;
COMMENT ON COLUMN wvp_user.default_password IS '是否使用默认密码';

-- 仅当 admin 的密码仍是默认密码时才标记 default_password=TRUE，已改过密码的不强制
UPDATE wvp_user
   SET default_password = TRUE
 WHERE username = 'admin'
   AND password = '21232f297a57a5a743894a0e4a801fc3';

/*
* 20261019 录像计划条目表字段改名，避免使用数据库关键字 start / stop
*/
DO $$
BEGIN
    IF TO_REGCLASS('wvp_record_plan_item') IS NOT NULL THEN
        IF EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_name = 'wvp_record_plan_item' AND column_name = 'start')
           AND NOT EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_name = 'wvp_record_plan_item' AND column_name = 'start_time') THEN
            ALTER TABLE wvp_record_plan_item RENAME COLUMN "start" TO start_time;
            EXECUTE 'COMMENT ON COLUMN wvp_record_plan_item.start_time IS ''开始时间（分钟）''';
        END IF;

        IF EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_name = 'wvp_record_plan_item' AND column_name = 'stop')
           AND NOT EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_name = 'wvp_record_plan_item' AND column_name = 'stop_time') THEN
            ALTER TABLE wvp_record_plan_item RENAME COLUMN stop TO stop_time;
            EXECUTE 'COMMENT ON COLUMN wvp_record_plan_item.stop_time IS ''结束时间（分钟）''';
        END IF;
    END IF;
END;
$$;
