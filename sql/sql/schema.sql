USE yumi_cute;

-- ① 用户表
CREATE TABLE `user` (
                        id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
                        username    VARCHAR(50)  NOT NULL                COMMENT '登录名',
                        password    VARCHAR(100) NOT NULL                COMMENT '加密后的密码',
                        nickname    VARCHAR(50)  NOT NULL DEFAULT ''     COMMENT '昵称',
                        avatar_url  VARCHAR(500) NOT NULL DEFAULT ''     COMMENT '头像地址',
                        points      INT          NOT NULL DEFAULT 0      COMMENT '算力余额',
                        vip_level   TINYINT      NOT NULL DEFAULT 0      COMMENT '会员等级',
                        status      TINYINT      NOT NULL DEFAULT 1      COMMENT '1正常 0禁用',
                        create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                        update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                        PRIMARY KEY (id),
                        UNIQUE KEY uk_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- ② 宠物表
CREATE TABLE pet (
                     id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
                     user_id     BIGINT       NOT NULL                COMMENT '所属用户id',
                     name        VARCHAR(30)  NOT NULL DEFAULT ''     COMMENT '宠物名字',
                     species     VARCHAR(20)  NOT NULL DEFAULT ''     COMMENT '种类 dog/cat',
                     breed       VARCHAR(50)  NOT NULL DEFAULT ''     COMMENT '品种',
                     avatar_url  VARCHAR(500) NOT NULL DEFAULT ''     COMMENT '头像地址',
                     create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                     PRIMARY KEY (id),
                     KEY idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='宠物表';

-- ③ 风格模板表
CREATE TABLE style_template (
                                id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
                                name        VARCHAR(50)  NOT NULL                COMMENT '风格名',
                                cover_url   VARCHAR(500) NOT NULL DEFAULT ''     COMMENT '封面图',
                                description VARCHAR(255) NOT NULL DEFAULT ''     COMMENT '简介',
                                prompt      TEXT         NULL                    COMMENT '给AI的提示词',
                                points_cost INT          NOT NULL DEFAULT 0      COMMENT '消耗算力',
                                sort_order  INT          NOT NULL DEFAULT 0      COMMENT '排序权重',
                                status      TINYINT      NOT NULL DEFAULT 1      COMMENT '1上架 0下架',
                                create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                PRIMARY KEY (id),
                                KEY idx_status_sort (status, sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='风格模板表';

-- ④ 写真订单表
CREATE TABLE photo_order (
                             id               BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
                             order_no         VARCHAR(32)  NOT NULL                COMMENT '对外订单号',
                             user_id          BIGINT       NOT NULL                COMMENT '下单用户',
                             pet_id           BIGINT       NOT NULL                COMMENT '拍摄对象',
                             style_id         BIGINT       NOT NULL                COMMENT '风格模板',
                             person_photo_url VARCHAR(500) NOT NULL DEFAULT ''     COMMENT '本人照片',
                             pet_photo_url    VARCHAR(500) NOT NULL DEFAULT ''     COMMENT '宠物照片',
                             order_type       TINYINT      NOT NULL DEFAULT 1      COMMENT '1试看单 2整套单',
                             total_count      INT          NOT NULL DEFAULT 0      COMMENT '总张数',
                             success_count    INT          NOT NULL DEFAULT 0      COMMENT '成功张数',
                             points_cost      INT          NOT NULL DEFAULT 0      COMMENT '消耗算力',
                             status           TINYINT      NOT NULL DEFAULT 0      COMMENT '0待处理 1生成中 2完成 3部分失败 4失败 5已取消',
                             create_time      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                             finish_time      DATETIME     NULL                    COMMENT '完成时间',
                             PRIMARY KEY (id),
                             UNIQUE KEY uk_order_no (order_no),
                             KEY idx_user_time (user_id, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='写真订单表';

-- ⑤ 生图任务表
CREATE TABLE photo_task (
                            id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
                            order_id    BIGINT       NOT NULL                COMMENT '所属订单',
                            seq_no      INT          NOT NULL DEFAULT 0      COMMENT '第几张',
                            status      TINYINT      NOT NULL DEFAULT 0      COMMENT '0待生成 1生成中 2成功 3失败',
                            image_url   VARCHAR(500) NOT NULL DEFAULT ''     COMMENT '结果图地址',
                            error_msg   VARCHAR(500) NOT NULL DEFAULT ''     COMMENT '失败原因',
                            retry_count INT          NOT NULL DEFAULT 0      COMMENT '重试次数',
                            create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                            update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                            PRIMARY KEY (id),
                            KEY idx_order_id (order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='生图任务表';

-- ⑥ 算力流水表
CREATE TABLE points_record (
                               id            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
                               user_id       BIGINT       NOT NULL                COMMENT '用户id',
                               change_amount INT          NOT NULL                COMMENT '正数增加 负数消耗',
                               balance_after INT          NOT NULL                COMMENT '变动后余额',
                               type          TINYINT      NOT NULL                COMMENT '1注册赠送 2生成消费 3充值 4退款',
                               biz_id        BIGINT       NOT NULL DEFAULT 0      COMMENT '关联业务id',
                               remark        VARCHAR(255) NOT NULL DEFAULT ''     COMMENT '备注',
                               create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                               PRIMARY KEY (id),
                               KEY idx_user_time (user_id, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='算力流水表';