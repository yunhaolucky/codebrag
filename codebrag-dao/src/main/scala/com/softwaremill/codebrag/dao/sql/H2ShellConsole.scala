package com.softwaremill.codebrag.dao.sql

import com.softwaremill.codebrag.dao.DaoConfig
import com.typesafe.config.ConfigFactory

object H2ShellConsole extends App {
  val config = new DaoConfig {
    def rootConfig = ConfigFactory.load()
  }

  println("Note: when selecting from tables, enclose the table name in \" \".")
  new org.h2.tools.Shell().runTool("-url", SQLDatabase.connectionString(config))
}
