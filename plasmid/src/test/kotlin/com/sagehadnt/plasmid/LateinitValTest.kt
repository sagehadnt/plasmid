package com.sagehadnt.plasmid

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class LateinitValTest {

    private lateinit var company: Company

    @BeforeEach
    fun setup() {
        company = Company()
    }

    @Test
    fun `cannot access value before setting`() {
        assertThatThrownBy { company.name }.hasMessage(NOT_SET_ERROR)
    }

    @Test
    fun `can set value`() {
        company.name = COMPANY_1
        assertThat(company.name).isEqualTo(COMPANY_1)
    }

    @Test
    fun `cannot change value`() {
        company.name = COMPANY_1
        assertThatThrownBy { company.name = COMPANY_2 }.hasMessage(ALREADY_SET_ERROR)
    }

    @Test
    fun `can reset value`() {
        company.name = COMPANY_1
        company.nameDelegate.reset()
        assertThatThrownBy { company.name }.hasMessage(NOT_SET_ERROR)
    }

    @Test
    fun `can reset value and then set it`() {
        company.name = COMPANY_1
        company.nameDelegate.reset()
        company.name = COMPANY_2
        assertThat(company.name).isEqualTo(COMPANY_2)
    }

}

private const val COMPANY_1 = "company1"
private const val COMPANY_2 = "company2"

private const val ALREADY_SET_ERROR = "already set"
private const val NOT_SET_ERROR = "not set"

private class Company {
    val nameDelegate = LateinitVal<String>(ALREADY_SET_ERROR, NOT_SET_ERROR)
    var name: String by nameDelegate
}