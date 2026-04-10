package com.dispatchops.infrastructure.persistence.mapper;

import com.dispatchops.domain.model.SearchIndex;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SearchIndexMapper {

    SearchIndex findByEntity(@Param("entityType") String entityType,
                             @Param("entityId") Long entityId);

    List<SearchIndex> search(@Param("query") String query,
                             @Param("entityType") String entityType,
                             @Param("authorId") Long authorId,
                             @Param("sort") String sort,
                             @Param("dateFrom") String dateFrom,
                             @Param("dateTo") String dateTo,
                             @Param("status") String status,
                             @Param("offset") int offset,
                             @Param("limit") int limit);

    int countSearch(@Param("query") String query,
                    @Param("entityType") String entityType,
                    @Param("authorId") Long authorId,
                    @Param("dateFrom") String dateFrom,
                    @Param("dateTo") String dateTo,
                    @Param("status") String status);

    List<SearchIndex> searchCourierScoped(@Param("query") String query,
                                            @Param("courierId") Long courierId,
                                            @Param("sort") String sort,
                                            @Param("dateFrom") String dateFrom,
                                            @Param("dateTo") String dateTo,
                                            @Param("status") String status,
                                            @Param("offset") int offset,
                                            @Param("limit") int limit);

    int countSearchCourierScoped(@Param("query") String query,
                                  @Param("courierId") Long courierId,
                                  @Param("dateFrom") String dateFrom,
                                  @Param("dateTo") String dateTo,
                                  @Param("status") String status);

    int upsert(SearchIndex idx);

    int delete(@Param("entityType") String entityType,
               @Param("entityId") Long entityId);
}
