package com.alecstrong.sql.psi.core.mysql.psi.mixins

import com.alecstrong.sql.psi.core.SqlAnnotationHolder
import com.alecstrong.sql.psi.core.mysql.psi.MySqlAlterTableChangeColumn
import com.alecstrong.sql.psi.core.psi.AlterTableApplier
import com.alecstrong.sql.psi.core.psi.LazyQuery
import com.alecstrong.sql.psi.core.psi.QueryElement
import com.alecstrong.sql.psi.core.psi.SqlColumnDef
import com.alecstrong.sql.psi.core.psi.SqlColumnName
import com.alecstrong.sql.psi.core.psi.SqlCompositeElementImpl
import com.alecstrong.sql.psi.core.psi.alterStmt
import com.intellij.lang.ASTNode

internal abstract class AlterTableChangeColumnMixin(
  node: ASTNode
) : SqlCompositeElementImpl(node),
    MySqlAlterTableChangeColumn,
    AlterTableApplier {
  private val columnName
      get() = children.filterIsInstance<SqlColumnName>().single()

  private val columnDef
    get() = children.filterIsInstance<SqlColumnDef>().single()

  override fun applyTo(lazyQuery: LazyQuery): LazyQuery {
    return LazyQuery(
        tableName = lazyQuery.tableName,
        query = {
          val columns = placementClause.placeInQuery(
              columns = lazyQuery.query.columns,
              column = QueryElement.QueryColumn(columnDef.columnName),
              replace = lazyQuery.query.columns.singleOrNull { (it.element as SqlColumnName).text == columnName.text }
          )
          lazyQuery.query.copy(columns = columns)
        }
    )
  }

  override fun annotate(annotationHolder: SqlAnnotationHolder) {
    super.annotate(annotationHolder)

    if (tablesAvailable(this)
            .filter { it.tableName.text == alterStmt.tableName?.text }
            .flatMap { it.query.columns }
            .none { (it.element as? SqlColumnName)?.text == columnName.text }) {
      annotationHolder.createErrorAnnotation(
          element = columnDef.columnName,
          s = "No column found to modify with name ${columnDef.columnName.text}"
      )
    }
  }
}
