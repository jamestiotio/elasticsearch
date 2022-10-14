/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.esql.plugin;

import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.support.ActionFilters;
import org.elasticsearch.action.support.HandledTransportAction;
import org.elasticsearch.cluster.metadata.IndexNameExpressionResolver;
import org.elasticsearch.cluster.service.ClusterService;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.compute.data.Block;
import org.elasticsearch.compute.data.Page;
import org.elasticsearch.search.SearchService;
import org.elasticsearch.tasks.Task;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.transport.TransportService;
import org.elasticsearch.xpack.esql.action.ColumnInfo;
import org.elasticsearch.xpack.esql.action.EsqlQueryAction;
import org.elasticsearch.xpack.esql.action.EsqlQueryRequest;
import org.elasticsearch.xpack.esql.action.EsqlQueryResponse;
import org.elasticsearch.xpack.esql.analyzer.Avg;
import org.elasticsearch.xpack.esql.execution.PlanExecutor;
import org.elasticsearch.xpack.esql.session.EsqlConfiguration;
import org.elasticsearch.xpack.esql.session.EsqlSession;
import org.elasticsearch.xpack.ql.expression.function.FunctionRegistry;

import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TransportEsqlQueryAction extends HandledTransportAction<EsqlQueryRequest, EsqlQueryResponse> {

    private final PlanExecutor planExecutor;
    private final ComputeService computeService;

    @Inject
    public TransportEsqlQueryAction(
        TransportService transportService,
        ActionFilters actionFilters,
        PlanExecutor planExecutor,
        IndexNameExpressionResolver indexNameExpressionResolver,
        SearchService searchService,
        ClusterService clusterService,
        ThreadPool threadPool
    ) {
        super(EsqlQueryAction.NAME, transportService, actionFilters, EsqlQueryRequest::new);
        this.planExecutor = planExecutor;
        this.computeService = new ComputeService(searchService, indexNameExpressionResolver, clusterService, threadPool);
    }

    @Override
    protected void doExecute(Task task, EsqlQueryRequest request, ActionListener<EsqlQueryResponse> listener) {
        // TODO: create more realistic function registry
        FunctionRegistry functionRegistry = new FunctionRegistry(FunctionRegistry.def(Avg.class, Avg::new, "AVG"));
        EsqlConfiguration configuration = new EsqlConfiguration(
            request.zoneId() != null ? request.zoneId() : ZoneOffset.UTC,
            null,
            null,
            x -> Collections.emptySet(),
            request.pragmas()
        );
        new EsqlSession(planExecutor.indexResolver(), functionRegistry, configuration).execute(request.query(), ActionListener.wrap(r -> {
            computeService.runCompute(r, configuration, listener.map(pages -> {
                List<ColumnInfo> columns = r.output().stream().map(c -> new ColumnInfo(c.qualifiedName(), c.dataType().esType())).toList();
                return new EsqlQueryResponse(columns, pagesToValues(pages));
            }));
        }, listener::onFailure));
    }

    private List<List<Object>> pagesToValues(List<Page> pages) {
        List<List<Object>> result = new ArrayList<>();
        for (Page page : pages) {
            for (int i = 0; i < page.getPositionCount(); i++) {
                List<Object> row = new ArrayList<>(page.getBlockCount());
                for (int b = 0; b < page.getBlockCount(); b++) {
                    Block block = page.getBlock(b);
                    row.add(block.getObject(i));
                }
                result.add(row);
            }
        }
        return result;
    }
}
