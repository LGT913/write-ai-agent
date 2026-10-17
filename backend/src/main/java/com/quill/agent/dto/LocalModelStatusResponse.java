package com.quill.agent.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

/**
 * 本地Ollama健康探测响应，对齐02-接口文档.md §2.1（v2.0三状态）
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record LocalModelStatusResponse(
        LocalModelStatusEnum statusEnum,
        boolean ollamaReachable,
        String targetModel,
        boolean modelExists,
        long machineRamGB,
        List<ModelSuggestion> suggestModelList,
        String pullCommand,
        String troubleshootHint
) {
    public record ModelSuggestion(String model, int ramMin, String desc) {}

    /** 服务就绪状态工厂 */
    public static LocalModelStatusResponse ready(String targetModel, long ramGB,
                                                 List<ModelSuggestion> suggestions) {
        return new LocalModelStatusResponse(
                LocalModelStatusEnum.READY,
                true,
                targetModel,
                true,
                ramGB,
                suggestions,
                null,
                "本地模型已就绪，可直接使用"
        );
    }

    /** 模型缺失状态工厂 */
    public static LocalModelStatusResponse modelMissing(String targetModel, long ramGB,
                                                        List<ModelSuggestion> suggestions,
                                                        String pullCommand) {
        return new LocalModelStatusResponse(
                LocalModelStatusEnum.MODEL_MISSING,
                true,
                targetModel,
                false,
                ramGB,
                suggestions,
                pullCommand,
                "目标模型未下载，请执行提示中的拉取命令完成安装"
        );
    }

    /** 服务不可达状态工厂（统一覆盖未安装/未启动场景） */
    public static LocalModelStatusResponse unavailable(String targetModel, long ramGB,
                                                       List<ModelSuggestion> suggestions) {
        return new LocalModelStatusResponse(
                LocalModelStatusEnum.UNAVAILABLE,
                false,
                targetModel,
                false,
                ramGB,
                suggestions,
                null,
                "无法连接 Ollama 服务，请确认后台服务已启动（托盘骆驼图标\uD83E\uDD99）"
        );
    }
    /** 通用工厂：仅供 Service 层特殊组合场景使用；常规三态请用上方语义化工厂 */
    public static LocalModelStatusResponse of (LocalModelStatusEnum statusEnum, boolean ollamaReachable,
                                               String targetModel, boolean modelExists, long machineRamGB,
                                               List<ModelSuggestion> suggestModelList,
                                               String pullCommand,
                                               String troubleshootHint) { return new LocalModelStatusResponse (statusEnum, ollamaReachable, targetModel,
            modelExists, machineRamGB, suggestModelList, pullCommand, troubleshootHint);
    }
}
