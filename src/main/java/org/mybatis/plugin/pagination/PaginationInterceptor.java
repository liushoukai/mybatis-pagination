package org.mybatis.plugin.pagination;

import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.Alias;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.select.SelectBody;
import net.sf.jsqlparser.statement.select.SelectExpressionItem;
import net.sf.jsqlparser.statement.select.SelectItem;
import net.sf.jsqlparser.statement.select.SelectVisitor;
import net.sf.jsqlparser.statement.select.SetOperationList;
import net.sf.jsqlparser.statement.select.SubSelect;
import net.sf.jsqlparser.statement.select.WithItem;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.mapping.ResultMap;
import org.apache.ibatis.mapping.SqlSource;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Plugin;
import org.apache.ibatis.plugin.Signature;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static java.lang.System.*;

/**
 * Created by KAYEN on 2016/11/17.
 */
@Intercepts(@Signature(
        type = Executor.class,
        method = "query",
        args = {
                MappedStatement.class,
                Object.class,
                RowBounds.class,
                ResultHandler.class
        })
)
public class PaginationInterceptor implements Interceptor {

    private static final List<SelectItem> COUNT_ITEM;
    private static final Alias TABLE_ALIAS;

    static {
        COUNT_ITEM = new ArrayList<SelectItem>();
        COUNT_ITEM.add(new SelectExpressionItem(new Column("count(0)")));

        TABLE_ALIAS = new Alias("table_count");
        TABLE_ALIAS.setUseAs(false);
    }

    @Override
    public Object intercept(Invocation invocation) throws Throwable {

        Object[] args = invocation.getArgs();

        MappedStatement ms = (MappedStatement)args[0];
        BoundSql boundSql =  ms.getBoundSql(args[1]);

        // judge from pagination param
        HashMap<String, Object> params = (HashMap<String, Object>)boundSql.getParameterObject();
        if (params.containsKey("pagination") && (params.get("pagination") instanceof Pagination)) {

            // remove pagiantion parameter mapping
            List<ParameterMapping> parameterMappings = new ArrayList<>();
            for(ParameterMapping x : boundSql.getParameterMappings()) {
                if (x.getProperty().startsWith("pagination")) continue;
                parameterMappings.add(x);
            }

            out.println(parameterMappings);

            // remove pagination parameter
            Map<String, Object> parameterObject = new HashMap<>();
            params.forEach((k,v) -> {
                if (!k.startsWith("pagination") && !(v instanceof Pagination) ) parameterObject.put(k, v);
            });

            out.println(parameterObject);

            String sql = boundSql.getSql();
            String countSql = this.getCountSQL(sql);
            BoundSql newBoundSql = new BoundSql(ms.getConfiguration(), countSql, parameterMappings, parameterObject);
            MappedStatement newMs = copyFromMappedStatement(ms, new BoundSqlSqlSource(newBoundSql));
            args[0] = newMs;

            out.println(invocation.getMethod());

            // invocation "public abstract Executor.query(MappedStatement, Object, RowBounds, ResultHandler)"
            List<Long> result = (List<Long>)invocation.proceed();
            long count = result.get(0);

            // set row count for pagination
            Pagination pagination = (Pagination) params.get("pagination");
            pagination.setTotal(count);

            args[0] = ms;
        }

        return invocation.proceed();
    }

    @Override
    public Object plugin(Object target) {
        return Plugin.wrap(target, this);
    }

    @Override
    public void setProperties(Properties properties) {

    }

    public static class BoundSqlSqlSource implements SqlSource {
        BoundSql boundSql;
        public BoundSqlSqlSource(BoundSql boundSql) {
            this.boundSql = boundSql;
        }
        public BoundSql getBoundSql(Object parameterObject) {
            return boundSql;
        }
    }

    private MappedStatement copyFromMappedStatement(MappedStatement ms, SqlSource newSqlSource) {
        MappedStatement.Builder builder = new MappedStatement.Builder(ms.getConfiguration(), ms.getId(), newSqlSource, ms.getSqlCommandType());
        builder.resource(ms.getResource());
        builder.fetchSize(ms.getFetchSize());
        builder.statementType(ms.getStatementType());
        builder.keyGenerator(ms.getKeyGenerator());
        // setStatementTimeout()
        builder.timeout(ms.getTimeout());
        // setParameterMap()
        builder.parameterMap(ms.getParameterMap());
        // setStatementResultMap()
        List<ResultMap> resultMaps = new ArrayList<ResultMap>();
        String id = "-inline";
        if (ms.getResultMaps() != null) {
            id = ms.getResultMaps().get(0).getId() + "-inline";
        }
        ResultMap resultMap = new ResultMap.Builder(null, id, Long.class, new ArrayList()).build();
        resultMaps.add(resultMap);
        builder.resultMaps(resultMaps);
        builder.resultSetType(ms.getResultSetType());
        // setStatementCache()
        builder.cache(ms.getCache());
        builder.flushCacheRequired(ms.isFlushCacheRequired());
        builder.useCache(ms.isUseCache());
        return builder.build();
    }

    private String getCountSQL(String sql) throws JSQLParserException {
        // 解析SQL
        Statement stmt = CCJSqlParserUtil.parse(sql);
        Select select = (Select) stmt;
        SelectBody selectBody = select.getSelectBody();
        PlainSelect plainSelect = new PlainSelect();
        SubSelect subSelect = new SubSelect();
        subSelect.setSelectBody(selectBody);
        subSelect.setAlias(TABLE_ALIAS);
        plainSelect.setFromItem(subSelect);
        plainSelect.setSelectItems(COUNT_ITEM);
        select.setSelectBody(plainSelect);
        selectBody.accept(new SelectVisitor() {
            @Override
            public void visit(PlainSelect plainSelect) {
                plainSelect.setLimit(null);
            }

            @Override
            public void visit(SetOperationList setOperationList) {}

            @Override
            public void visit(WithItem withItem) {}
        });
        sql = select.toString();
        return sql;
    }
}
