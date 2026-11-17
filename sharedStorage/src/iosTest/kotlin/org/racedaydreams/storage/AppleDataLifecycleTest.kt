package org.racedaydreams.storage

class AppleDataLifecycleTest : DataLifecycleRepositoryContract() {
    override fun database(): SqlDatabase = AppleSqliteDatabase(":memory:")

    override fun directory(): String = platform.Foundation.NSTemporaryDirectory()
}
