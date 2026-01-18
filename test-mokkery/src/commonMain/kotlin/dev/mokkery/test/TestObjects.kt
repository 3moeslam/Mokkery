package dev.mokkery.test

/**
 * Test fixtures for object mocking functionality.
 * These objects are used to test mockObject<T>() feature.
 */

/**
 * Simple service object with basic functions for testing.
 */
object TestService {

    fun getData(): String = "real data"

    fun processData(input: String): String = "processed: $input"

    fun saveData(data: String): Boolean = true

    fun getCount(): Int = 42

    val staticValue: String = "static"

    var mutableValue: String = "initial"
}

/**
 * Object with suspend functions for coroutine testing.
 */
object AsyncTestService {

    suspend fun fetchData(): String = "async data"

    suspend fun fetchWithDelay(delayMs: Long): String {
        // In real implementation would delay
        return "delayed result"
    }

    suspend fun processAsync(input: String): Result<String> = Result.success("async: $input")
}

/**
 * Object with complex types for advanced testing.
 */
object ComplexTestService {

    fun getComplexType(): ComplexType = ComplexType

    fun processComplexType(input: ComplexType): ComplexType = input

    fun getList(): List<String> = listOf("a", "b", "c")

    fun getMap(): Map<String, Int> = mapOf("a" to 1, "b" to 2)

    fun <T> genericFunction(input: T): T = input
}

/**
 * Object with properties for property mocking tests.
 */
object PropertyTestService {

    val readOnlyProperty: String = "read-only"

    var mutableProperty: Int = 0

    val computedProperty: String
        get() = "computed"
}

/**
 * Counter object for parallel test isolation verification.
 */
object Counter {

    private var count = 0

    val value: Int
        get() = count

    fun increment(): Int = ++count

    fun decrement(): Int = --count

    fun reset() {
        count = 0
    }
}

/**
 * Class with companion object for companion mocking tests.
 */
class ServiceFactory private constructor(val name: String) {

    companion object {
        fun create(): ServiceFactory = ServiceFactory("default")

        fun createWithName(name: String): ServiceFactory = ServiceFactory(name)

        val defaultInstance: ServiceFactory = ServiceFactory("singleton")
    }
}

/**
 * Another class with companion for testing multiple companion objects.
 */
class Repository private constructor(val id: String) {

    fun query(): String = "result for $id"

    companion object {
        fun getInstance(): Repository = Repository("default-repo")

        fun getInstanceById(id: String): Repository = Repository(id)
    }
}

/**
 * Object with internal function calls for testing internal interception.
 */
object InternalCallTestService {

    fun publicFunction(): String {
        return internalHelper()
    }

    private fun internalHelper(): String = "internal result"

    fun callsAnother(): String {
        return anotherPublic()
    }

    fun anotherPublic(): String = "another result"
}

/**
 * Object with overloaded functions.
 */
object OverloadedTestService {

    fun process(input: Int): Int = input * 2

    fun process(input: String): String = input.uppercase()

    fun process(input: Double): Double = input * 2.0

    fun process(first: Int, second: Int): Int = first + second
}
