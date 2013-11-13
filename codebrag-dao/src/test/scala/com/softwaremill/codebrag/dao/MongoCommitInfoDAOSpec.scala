package com.softwaremill.codebrag.dao

import org.scalatest.matchers.ShouldMatchers
import org.joda.time.DateTime
import com.softwaremill.codebrag.domain.builder.{UserAssembler, CommitInfoAssembler}
import CommitInfoAssembler._
import com.softwaremill.codebrag.test.mongo.ClearDataAfterTest
import com.softwaremill.codebrag.domain.User

class MongoCommitInfoDAOSpec extends FlatSpecWithMongo with ClearDataAfterTest with ShouldMatchers {
  var commitInfoDAO: MongoCommitInfoDAO = _
  val FixtureTime = new DateTime(23333333)
  override def beforeEach() {
    super.beforeEach()
    commitInfoDAO = new MongoCommitInfoDAO
  }

  it should "find a stored commit" taggedAs(RequiresDb) in {
    // given
    val commit = randomCommit.get
    commitInfoDAO.storeCommit(commit)

    // when
    val foundCommit = commitInfoDAO.findBySha(commit.sha)

    // then
    foundCommit should be(Some(commit.copy()))
  }

  it should "find stored commit by its id" taggedAs(RequiresDb) in {
    // given
    val commit = randomCommit.get
    commitInfoDAO.storeCommit(commit)

    // when
    val foundCommit = commitInfoDAO.findByCommitId(commit.id)

    // then
    foundCommit should be(Some(commit.copy()))
  }

  it should "store a single commit" taggedAs(RequiresDb) in {
    // given
    val commit = randomCommit.get

    // when
    commitInfoDAO.storeCommit(commit)

    // then
    commitInfoDAO.findBySha(commit.sha) should be('defined)
  }

  it should "return false in hasCommits when empty" taggedAs(RequiresDb) in {
    // given empty db

    // then
    commitInfoDAO.hasCommits should be(false)
  }

  it should "return true in hasCommits when not empty" taggedAs(RequiresDb) in {
    // given
    commitInfoDAO.storeCommit(randomCommit.get)

    // then
    commitInfoDAO.hasCommits should be(true)
  }

  it should "retrieve commit sha with last commit + author date" taggedAs(RequiresDb) in {
    // given
    val date = new DateTime()

    val expectedLastCommit = randomCommit.withAuthorDate(date.minusDays(2)).withCommitDate(date).get
    commitInfoDAO.storeCommit(randomCommit.withAuthorDate(date.minusDays(3)).withCommitDate(date).get)
    commitInfoDAO.storeCommit(randomCommit.withAuthorDate(date.minusHours(12)).withCommitDate(date.minusHours(13)).get)
    commitInfoDAO.storeCommit(expectedLastCommit)
    commitInfoDAO.storeCommit(randomCommit.withAuthorDate(date.minusDays(11)).withCommitDate(date).get)
    commitInfoDAO.storeCommit(randomCommit.withAuthorDate(date.minusHours(6)).withCommitDate(date.minusHours(8)).get)
    commitInfoDAO.storeCommit(randomCommit.withAuthorDate(date.minusHours(10)).withCommitDate(date.minusHours(11)).get)

    // when
    val lastSha = commitInfoDAO.findLastSha()

    // then
    lastSha should not be (null)
    lastSha should equal (Some(expectedLastCommit.sha))
  }

  it should "find all commits SHA" taggedAs(RequiresDb) in {
    // given
    val commits = List(CommitInfoAssembler.randomCommit.withSha("111").get, CommitInfoAssembler.randomCommit.withSha("222").get)
    commits.foreach {
      commitInfoDAO.storeCommit(_)
    }

    // when
    val commitsSha = commitInfoDAO.findAllSha()

    // then
    commitsSha should equal(commits.map(_.sha).toSet)
  }

  it should "find last commits (ordered) for user" taggedAs(RequiresDb) in {
    // given
    val tenDaysAgo = DateTime.now.minusDays(10)
    val John = UserAssembler.randomUser.withEmail("john@codebrag.com").get
    val Bob = UserAssembler.randomUser.withFullName("Bob Smith").get

    val commits = List(
      buildCommit(user = John, date = tenDaysAgo.plusDays(1), sha = "1"),
      buildCommit(user = Bob, date = tenDaysAgo.plusDays(2), sha = "2"),
      buildCommit(user = John, date = tenDaysAgo.plusDays(3), sha = "3"),
      buildCommit(user = Bob, date = tenDaysAgo.plusDays(4), sha = "4"),
      buildCommit(user = Bob, date = tenDaysAgo.plusDays(5), sha = "5"),
      buildCommit(user = John, date = tenDaysAgo.plusDays(6), sha = "6"),
      buildCommit(user = John, date = tenDaysAgo.plusDays(7), sha = "7")
    )
    commits.foreach(commitInfoDAO.storeCommit)
    
    // when
    val threeCommitsNotByJohn = commitInfoDAO.findNewestCommitsNotAuthoredByUser(John, 3)
    val atMostTenCommitsNotByBob = commitInfoDAO.findNewestCommitsNotAuthoredByUser(Bob, 10)

    // then
    threeCommitsNotByJohn.map(_.sha) should be(List("5", "4", "2"))
    atMostTenCommitsNotByBob.map(_.sha) should be(List("7", "6", "3", "1"))
  }

  it should "find last commit authored by user" in {
    // given
    val tenDaysAgo = DateTime.now.minusDays(10)
    val John = UserAssembler.randomUser.withEmail("john@codebrag.com").get
    val Bob = UserAssembler.randomUser.withFullName("Bob Smith").get
    val Alice = UserAssembler.randomUser.withFullName("Alice Smith").get

    val commits = List(
      buildCommit(user = John, date = tenDaysAgo.plusDays(1), sha = "1"),
      buildCommit(user = Bob, date = tenDaysAgo.plusDays(2), sha = "2"),
      buildCommit(user = Bob, date = tenDaysAgo.plusDays(4), sha = "4"),
      buildCommit(user = John, date = tenDaysAgo.plusDays(6), sha = "6")
    )
    commits.foreach(commitInfoDAO.storeCommit)

    // when
    val Some(lastCommitByBob) = commitInfoDAO.findLastCommitAuthoredByUser(Bob)
    val Some(lastCommitByJohn) = commitInfoDAO.findLastCommitAuthoredByUser(John)
    val noCommitByAlice = commitInfoDAO.findLastCommitAuthoredByUser(Alice)

    // then
    lastCommitByBob.sha should be("4")
    lastCommitByJohn.sha should be("6")
    noCommitByAlice should be(None)
  }

  def buildCommit(user: User, date: DateTime, sha: String) = {
    CommitInfoAssembler.randomCommit.withAuthorEmail(user.email).withAuthorName(user.name).withAuthorDate(date).withSha(sha).get
  }
  
}
