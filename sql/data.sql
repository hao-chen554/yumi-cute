-- ============================================================
--  YuMi-cute 初始化数据
--
--  用法：
--    mysql -uroot -p < sql/schema.sql          -- 先建表
--    mysql -uroot -p yumi_cute < sql/data.sql  -- 再灌数据
--
--  没有这些数据：首页的风格列表是空的，用户根本无法下单。
-- ============================================================

USE yumi_cute;

-- 强制本会话使用 utf8mb4。
-- Windows 上的 mysql 客户端默认拿控制台编码（GBK）当连接字符集，
-- 不加这行的话，下面这些中文会直接报
--   ERROR 1366: Incorrect string value ... for column 'description'
-- 或者被静默存成乱码。
SET NAMES utf8mb4;

-- ------------------------------------------------------------
--  官方风格模板（8 套）
--
--  注意 prompt 的写法：它只描述"画面长什么样"（光线、色调、质感、场景）。
--  「保留人物面部特征」「保留宠物毛色花纹」「不许画成拼图」这些硬约束
--  由后端 AiPhotoGenerator.buildPrompt() 统一拼接 —— 所以这里不需要重复写。
--
--  为什么用英文：官方文档说明图像模型对英文提示词的遵循度更高。
-- ------------------------------------------------------------
INSERT INTO style_template (id, name, cover_url, description, prompt, points_cost, sort_order, status) VALUES
(1, '日系清新', '', '柔和自然光，清透色调，午后的温柔时光',
 'soft natural window light, pastel tones, gentle mood, film grain, cozy japanese interior, person with pet, 85mm portrait lens, high detail',
 20, 1, 1),

(2, '复古胶片', '', '颗粒感与暖色调，像老照片里的旧时光',
 'vintage film photography, kodak portra, warm tones, heavy grain, faded colors, nostalgic mood, person with pet, analog camera',
 20, 2, 1),

(3, '黑白肖像', '', '纯粹的光影，只留下你们之间的情绪',
 'black and white portrait, dramatic side lighting, high contrast, monochrome fine art photography, person with pet, studio backdrop',
 20, 3, 1),

(4, '国风工笔', '', '东方笔触与留白，把你们画进一幅古画',
 'traditional chinese painting style, gongbi brushwork, ink wash, muted jade and beige palette, classical garden, person with pet',
 30, 4, 1),

(5, '梦幻星空', '', '银河为幕，让这一刻变得不真实',
 'dreamy starry night sky, milky way background, magical glow, deep blue and purple palette, cinematic lighting, person with pet',
 25, 5, 1),

(6, '圣诞主题', '', '暖黄的灯与雪，一年一次的仪式感',
 'christmas theme, warm fairy lights, snow, cozy sweaters, red and green accents, bokeh background, person with pet, festive',
 25, 6, 1),

(7, '生日派对', '', '彩带、蛋糕，和你们一起吹蜡烛',
 'birthday party, confetti, balloons, cake with candles, bright cheerful colors, playful mood, person with pet',
 25, 7, 1),

(8, '咖啡馆日常', '', '像某个周末下午随手拍下的生活片段',
 'cozy cafe interior, afternoon sunlight through window, coffee cup on table, warm wood tones, candid lifestyle photography, person with pet',
 20, 8, 1);
