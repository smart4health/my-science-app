package com.healthmetrix.myscience.feature.sync

import org.hl7.fhir.r4.model.Base
import org.hl7.fhir.r4.model.Resource

fun Resource.visualize() {
    (this as Base)
        .visualize(0)
        .asReversed()
        .forEach(::println)
}

fun Base.visualize(
    indentLevel: Int,
    indexInfo: IndexInfo? = null,
    propName: String? = null,
): List<String> {
    val output = children().map { prop ->
        prop.values.mapIndexed { i, value ->
            val propIndexInfo = if (prop.isList) {
                IndexInfo(i, prop.values.size)
            } else {
                null
            }
            value.visualize(indentLevel + 1, propIndexInfo, prop.name)
        }
    }.flatten().asReversed().flatten()

    val spacing = " ".repeat(indentLevel * 4)
    val name = propName?.let { " $it" } ?: ""
    val indexInfoRendered = indexInfo?.let { " $it" } ?: ""

    // ┌──
    // └──
    val newOutput = ("$spacing└── ${this::class.simpleName}${name}$indexInfoRendered")

    return output + newOutput
}

data class IndexInfo(val index: Int, val of: Int) {
    override fun toString() = "[${index + 1}/$of]"
}
