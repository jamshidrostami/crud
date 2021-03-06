package org.bardframework.base.datatable;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.bardframework.commons.jackson.converter.PersianStringDisinfectant;
import org.bardframework.commons.utils.CollectionUtils;
import org.bardframework.commons.utils.StringUtils;

import java.util.List;

/**
 * Created by v.zafari on 11/14/2015.
 */
public class DataTableFilter<F extends DataTableFilter<F>> {

    protected long page;
    protected long count;
    @JsonDeserialize(using = PersianStringDisinfectant.class)
    protected String query;
    protected List<HeaderDto> headers;

    public DataTableFilter() {
    }

    public DataTableFilter(List<HeaderDto> headers) {
        this.headers = headers;
    }

    public List<HeaderDto> getHeaders() {
        return headers;
    }

    public void setHeaders(List<HeaderDto> headers) {
        this.headers = headers;
    }

    public long getCount() {
        return count;
    }

    /**
     * @param count
     * @return <code>this</code> for method chaining
     */
    public F setCount(long count) {
        this.count = count;
        return (F) this;
    }

    public long getPage() {
        return page;
    }

    /**
     * @param page
     * @return <code>this</code> for method chaining
     */
    public F setPage(long page) {
        this.page = page;
        return (F) this;
    }

    public String getQuery() {
        return query;
    }

    public F setQuery(String query) {
        this.query = query;
        return (F) this;
    }

    @JsonIgnore
    public boolean isEmpty() {
        return (0 == count || 10 == count) && page < 2 && !StringUtils.hasText(query) && (CollectionUtils.isEmpty(headers) || headers.stream().allMatch(o -> o.isEmpty()));
    }
}