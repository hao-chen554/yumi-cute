package com.yumi.cute.generator;

import com.yumi.cute.entity.PhotoOrder;
import com.yumi.cute.entity.PhotoTask;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Component
public class MockPhotoGenerator implements PhotoGenerator {

    /** 模拟每一张图的生成耗时（毫秒） */
    private static final long FAKE_COST_MS = 2000;

    /** 模拟失败概率。想让它全部成功，把这里改成 0 */
    private static final double FAIL_RATE = 0.12;

    @Override
    public String generate(PhotoOrder order, PhotoTask task) {

        try {
            Thread.sleep(FAKE_COST_MS);
        } catch (InterruptedException e) {
            // 恢复中断标记，别把这个异常吞掉
            Thread.currentThread().interrupt();
            throw new RuntimeException("生成被中断");
        }

        if (ThreadLocalRandom.current().nextDouble() < FAIL_RATE) {
            throw new RuntimeException("模型服务暂时不可用");
        }

        // 还没接真实 AI，先把用户上传的原图当作结果
        return order.getPersonPhotoUrl();
    }
}