package org.racedaydreams.domain.activity

/** Existing typed persistence boundary; platform owns IO and transactions. */
interface ResultRepository {
    fun results(): List<RecordedResult>

    fun saveResult(result: RecordedResult)

    fun deleteResult(id: String)
}
