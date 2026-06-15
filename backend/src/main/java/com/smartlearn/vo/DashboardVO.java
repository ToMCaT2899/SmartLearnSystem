package com.smartlearn.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardVO {
    private List<StatCardVO> stats;
    private ChartDataVO barChart;
    private List<PieItemVO> pieChart;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StatCardVO {
        private String title;
        private String value;
        private String icon;
        private String gradient;
        private Double trend;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChartDataVO {
        private List<String> days;
        private List<Integer> vals;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PieItemVO {
        private Integer value;
        private String name;
    }

}
