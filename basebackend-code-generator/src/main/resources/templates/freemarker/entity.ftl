package ${packageName}.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.basebackend.database.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
<#if hasDateTime>
import java.time.LocalDateTime;
import java.time.LocalDate;
</#if>
<#if hasBigDecimal>
import java.math.BigDecimal;
</#if>

/**
 * ${tableComment}实体
 * 
 * @author ${author}
 * @date ${date}
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("${tableName}")
public class ${className} extends BaseEntity {

<#list columns as column>
<#if !column.isSystemField>
    /**
     * ${column.columnComment}
     */
    @TableField("${column.columnName}")
    private ${column.javaType} ${column.javaField};

</#if>
</#list>
}
