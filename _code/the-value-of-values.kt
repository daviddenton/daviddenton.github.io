import dev.forkhandles.values.BigDecimalValueFactory
import dev.forkhandles.values.Value
import dev.forkhandles.values.minValue
import dev.forkhandles.values.ofListResult
import dev.forkhandles.values.ofResult
import java.math.BigDecimal
import java.math.BigDecimal.ZERO
import java.math.MathContext
import java.util.Currency

interface Sigs {
    fun transferMoneyTo(amount: BigDecimal, sortCode: String, account: String)
}

interface Data {
    data class Amount(val value: BigDecimal)
    data class SortCode(val value: String)
    data class Account(val value: String)
}

interface AsValue {
    @JvmInline
    value class Amount(val value: BigDecimal)

    @JvmInline
    value class SortCode(val value: String)

    @JvmInline
    value class Account(val value: String)
}

object Construction {
    @JvmInline
    value class Amount(val value: BigDecimal) {
        init {
            require(value > ZERO)
        }
    }

    object better {
        @JvmInline
        value class Amount private constructor(val value: BigDecimal) {
            companion object {
                fun of(value: BigDecimal): Result<Amount> =
                    if (value > ZERO) Result.success(Amount(value.round(MathContext(3))))
                    else Result.failure(IllegalArgumentException("less than zero"))
            }
        }

        val onePointTwoSeven: Result<Amount> = Amount.of(BigDecimal("1.271"))
    }

    object values4k {
        @JvmInline
        value class Amount private constructor(override val value: BigDecimal) : Value<BigDecimal> {
            companion object : BigDecimalValueFactory<Amount>(
                { Amount(it.round(MathContext(3))) }, BigDecimal("0.01").minValue
            )
        }

        val onePointTwoSeven: Result<Amount> = Amount.ofResult(BigDecimal("1.271"))
        val values: Result<List<Amount>> = Amount.ofListResult(BigDecimal("1.271"), BigDecimal("2.567"))
    }
}

object Functions {
    @JvmInline
    value class Amount(val value: BigDecimal) {
        operator fun plus(that: Amount) = Amount((value + that.value))
    }

    val eleven = Amount(BigDecimal(5)) + Amount(BigDecimal(10))

    fun Amount.taxedAt(percent: Int) = Amount(value * BigDecimal((1 - percent / 100.0)))
    val eighty = Amount(BigDecimal(100)).taxedAt(20)
}

object composition {
    @JvmInline
    value class Amount(val value: BigDecimal)

    operator fun Amount.plus(that: Amount) = Amount((value + that.value))

    data class Money(val amount: Amount, val currency: Currency) {
        operator fun plus(that: Money): Money {
            require(currency == that.currency)
            return Money(amount + that.amount, currency)
        }
    }
}

object Parsing {
    object better {
        @JvmInline
        value class Amount private constructor(val value: BigDecimal) {
            companion object {
                fun of(value: BigDecimal): Result<Amount> =
                    if (value > ZERO) Result.success(Amount(value.round(MathContext(3))))
                    else Result.failure(IllegalArgumentException("less than zero"))

                fun parse(value: String) = runCatching { of(BigDecimal(value.dropLast(1))).getOrThrow() }
            }
        }

        val onePointTwoSeven: Result<Amount> = Amount.parse("1.271!")

        @JvmStatic
        fun main(args: Array<String>) {
            println(onePointTwoSeven)
        }
    }
}

object Showing {
    object better {
        @JvmInline
        value class Amount private constructor(val value: BigDecimal) {
            companion object {
                fun of(value: BigDecimal): Result<Amount> =
                    if (value > ZERO) Result.success(Amount(value.round(MathContext(3))))
                    else Result.failure(IllegalArgumentException("less than zero"))

                fun show(amount: Amount) = "${amount.value}!"
            }
        }

        val amount = Amount.of(BigDecimal("1.267")).getOrThrow()
        val onePointTwoSeven = Amount.show(amount)

        @JvmStatic
        fun main(args: Array<String>) {
            println(onePointTwoSeven)
        }
    }

}

object values4k {
    @JvmInline
    value class Amount private constructor(override val value: BigDecimal) : Value<BigDecimal> {
        companion object : BigDecimalValueFactory<Amount>(
            { Amount(it.round(MathContext(3))) }, BigDecimal("0.01").minValue
        )
    }

    val onePointTwoSeven: Result<Amount> = Amount.ofResult(BigDecimal("1.271"))
    val values: Result<List<Amount>> = Amount.ofListResult(BigDecimal("1.271"), BigDecimal("2.567"))
}

@JvmInline
value class Bob(val value: String)

fun main() {
    println(Bob("123123"))
}
