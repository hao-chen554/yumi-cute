package com.yumi.cute.generator;

import com.yumi.cute.entity.PhotoOrder;
import com.yumi.cute.entity.PhotoTask;
import com.yumi.cute.entity.StyleTemplate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Component
@ConditionalOnProperty(name = "ai.provider", havingValue = "mock", matchIfMissing = true)
public class MockPhotoGenerator implements PhotoGenerator {

    /**
     * 模拟每一张图的生成耗时（毫秒）
     */
    private static final long FAKE_COST_MS = 2000;

    /**
     * 模拟"模型少给几张"的概率。
     * 默认 0 = 全部成功。想验证"部分失败"分支时改成 0.5。
     */
    private static final double FAIL_RATE = 0.01;

    @Override
    public List<String> generate(PhotoOrder order, StyleTemplate style) {
        int total = order.getTotalCount() == null ? 1 : order.getTotalCount();
        ArrayList<String> urls = new ArrayList<>(total);

        log.info("[MOCK]开始生成{}张，风格是{}", total, style.getName());

        for (int i = 1; i <= total; i++) {
            //模拟生图用时
            sleep();

            if (ThreadLocalRandom.current().nextDouble() < FAIL_RATE) {
                //这张用于模拟“部分失败”的分派逻辑
                log.info("[MOCK]生成{}/{}张失败", i, total);
                continue;
            }

            //直接返回用户上传的原图进行模拟
            urls.add(order.getPersonPhotoUrl());
        }

        log.info("[MOCK]生成结束，成功{}/{}张", urls.size(), total);
        return urls;
    }

    private void sleep() {
        try {
            Thread.sleep(FAKE_COST_MS);
        } catch (InterruptedException e) {
            //恢复中断标记，别把这个异常吞掉
            Thread.currentThread().interrupt();
            throw new RuntimeException("生成被中断");
        }
    }
}