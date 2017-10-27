package io.bluebank.hermes.sample

import io.bluebank.hermes.server.ServiceDescription
import java.util.concurrent.atomic.AtomicInteger

@ServiceDescription(name = "accounts", description = "account service")
class AccountService() {
  private val accounts : MutableMap<String, Account> = mutableMapOf()
  private val nextId = AtomicInteger(1)

  fun createAccount(name: String): Account {
    val account = Account(id = nextId.getAndIncrement().toString(), name = name)
    accounts.put(account.id, account)
    return account
  }

  fun getAccounts() : Map<String, Account> {
    return accounts.toMap()
  }

  fun updateAccount(account: Account) {
    accounts[account.id] = account
  }
  fun clearAccounts() {
    accounts.clear()
  }
}

data class Account(val id: String, val name: String)