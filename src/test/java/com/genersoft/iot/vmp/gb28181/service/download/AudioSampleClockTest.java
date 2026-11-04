package com.genersoft.iot.vmp.gb28181.service.download;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static com.genersoft.iot.vmp.gb28181.service.download.SegmentedRecordingAssembler.AudioFrame;
import static com.genersoft.iot.vmp.gb28181.service.download.SegmentedRecordingAssembler.SampleRange;

/** 原样本索引必须守恒；时间戳误差应由共同采样时钟证明，不能逐帧累加容差。 */
class AudioSampleClockTest {
    private final SegmentedRecordingAssembler assembler = new SegmentedRecordingAssembler(new SegmentedDownloadSettings());

    /** 一帧晚2ms后恢复能满足每次测量原±1.1ms预算，后一帧不得再丢32个真实样本。 */
    @Test
    void recoveredTwoMillisecondJitterKeepsEveryRealSample() throws Exception {
        var plan = assembler.planAudioSamples(frames(0, .064, .130, .192, .256), 16000, .001, .321);
        assertEquals(5120, plan.total());assertEquals(5120, plan.selected());assertEquals(0, plan.discarded());
        assertEquals(List.of(new SampleRange(0, 5120)), plan.ranges());
        assertTrue(plan.clock().lower() <= plan.clock().upper());
        assertEquals(.001, plan.clock().anchor(), 1e-9);
        assertTrue(plan.clock().maxError() <= .0011);
    }

    /** 起始重叠与中段2ms往返抖动同时出现时，去重仍只有原1008样本。 */
    @Test
    void initialOverlapAndRecoveredJitterDoNotDiscardExtraSamples() throws Exception {
        var plan = assembler.planAudioSamples(frames(0, .001, .065, .131, .193, .257), 16000, .001, .322);
        assertEquals(6144, plan.total());assertEquals(5136, plan.selected());assertEquals(1008, plan.discarded());
        assertEquals(List.of(new SampleRange(0, 1024), new SampleRange(2032, 6144)), plan.ranges());
        assertEquals(1008, plan.clock().prefixOverlap());
        assertTrue(plan.clock().maxError() <= .0011);
    }

    /** 第二帧早2ms后恢复也应先证明共同clock，不能按起始重叠额外删除32样本。 */
    @Test
    void initialRecoveredNegativeJitterIsNotTreatedAsDuplicateSamples() throws Exception {
        var plan = assembler.planAudioSamples(frames(0, .062, .128, .192), 16000, .032, .160);
        assertEquals(4096, plan.total());assertEquals(2048, plan.selected());assertEquals(0, plan.discarded());
        assertEquals(0, plan.clock().prefixOverlap());
        assertEquals(List.of(new SampleRange(528, 2576)), plan.ranges());
        assertEquals(-.001, plan.clock().anchor(), 1e-9);
    }

    /** 相邻时钟各自看似微小的误差不能无限累积，正负漂移都需统一证明。 */
    @ParameterizedTest
    @ValueSource(ints = {-1, 1})
    void rejectsAccumulatedSubMillisecondDrift(int direction) {
        List<AudioFrame> frames = new ArrayList<>();
        for (int i = 0; i < 12; i++) frames.add(new AudioFrame(i * .064 + direction * i * .0005, 1024));
        assertThrows(IOException.class, () -> assembler.planAudioSamples(frames, 16000, .01, .7));
    }

    /** 没有共同测量区间的3ms跳动以及真正少一帧AAC，都不能补样本凑连续。 */
    @Test
    void rejectsThreeMillisecondJitterAndMissingAacFrame() {
        assertThrows(IOException.class, () -> assembler.planAudioSamples(frames(0, .064, .131, .192, .256), 16000, .001, .32));
        assertThrows(IOException.class, () -> assembler.planAudioSamples(frames(0, .064, .192, .256), 16000, .001, .32));
    }

    /** 既有63ms起始重叠仅剔除1008个已覆盖样本，剩余原始字节顺序保持。 */
    @Test
    void keepsEstablishedInitialOverlapAndPhysicalSampleOrder() throws Exception {
        var plan = assembler.planAudioSamples(frames(0, .001, .065, .129, .193), 16000, 0, .257);
        assertEquals(5120, plan.total());assertEquals(4112, plan.selected());assertEquals(1008, plan.discarded());
        assertEquals(List.of(new SampleRange(0, 1024), new SampleRange(2032, 5120)), plan.ranges());
    }

    /** 任意中途大重叠不能伪装成起始重叠而反复丢样本绕过共同时钟。 */
    @Test
    void rejectsUnexplainedInteriorOverlap() {
        assertThrows(IOException.class, () -> assembler.planAudioSamples(frames(0, .064, .065, .129, .193), 16000, 0, .25));
    }

    /** 真实非零起点和不足尾部仍由原覆盖门槛检查，不能重置成零或借时钟平移掩盖。 */
    @Test
    void preservesRealAudioStartAndTailCoverageChecks() {
        assertThrows(IOException.class, () -> assembler.planAudioSamples(frames(.003, .067, .131, .195), 16000, 0, .25));
        assertThrows(IOException.class, () -> assembler.planAudioSamples(frames(0, .064, .128), 16000, 0, .25));
    }

    /** 被裁掉区域的真间隙不影响其后独立连续块，但不能把该间隙搬入选取范围。 */
    @Test
    void selectedContinuousBlockAfterAnEarlierGapKeepsAbsolutePts() throws Exception {
        var input = frames(0, .064, .328, .392, .456);
        var plan = assembler.planAudioSamples(input, 16000, .34, .48);
        assertEquals(2240, plan.selected());assertEquals(List.of(new SampleRange(2240, 4480)), plan.ranges());
        assertThrows(IOException.class, () -> assembler.planAudioSamples(input, 16000, .12, .48));
    }

    /** 只有解码元数据已证明的真实帧参与选择。 */
    private List<AudioFrame> frames(double... pts) {
        List<AudioFrame> result = new ArrayList<>();
        for (double value : pts) result.add(new AudioFrame(value, 1024));
        return result;
    }
}
