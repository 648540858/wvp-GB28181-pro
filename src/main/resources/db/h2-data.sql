/*初始数据*/
INSERT INTO wvp_user (id, username, password, role_id, create_time, update_time, push_key)
SELECT 1, 'admin', '21232f297a57a5a743894a0e4a801fc3', 1, '2021-04-13 14:14:57', '2021-04-13 14:14:57',
       '3e80d1762a324d5b0ff636e0bd16f1e3'
WHERE NOT EXISTS (SELECT 1 FROM wvp_user WHERE id = 1);
INSERT INTO wvp_user_role (id, name, authority, create_time, update_time)
SELECT 1, 'admin', '0', '2021-04-13 14:14:57', '2021-04-13 14:14:57'
WHERE NOT EXISTS (SELECT 1 FROM wvp_user_role WHERE id = 1);
