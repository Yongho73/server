package com.noah.api.config.database;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.ttddyy.dsproxy.ExecutionInfo;
import net.ttddyy.dsproxy.QueryInfo;
import net.ttddyy.dsproxy.listener.QueryExecutionListener;

// 커스텀 리스너 정의
public class SimpleQueryLoggingListener implements QueryExecutionListener {
    private static final Logger logger = LoggerFactory.getLogger("SQL");

    @Override
    public void beforeQuery(ExecutionInfo execInfo, List<QueryInfo> queryInfoList) {}

    @Override
    public void afterQuery(ExecutionInfo execInfo, List<QueryInfo> queryInfoList) {
        for (QueryInfo queryInfo : queryInfoList) {
            logger.debug("SQL: {}", queryInfo.getQuery());
            if (!queryInfo.getParametersList().isEmpty()) {
                logger.debug("Params: {}", queryInfo.getParametersList());
            }
        }
    }
}
