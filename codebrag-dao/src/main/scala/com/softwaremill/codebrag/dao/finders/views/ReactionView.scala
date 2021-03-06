package com.softwaremill.codebrag.dao.finders.views

import org.joda.time.DateTime

case class CommitReactionsView(entireCommitReactions: ReactionsView, inlineReactions: Map[String, Map[String, ReactionsView]])

case class ReactionsView(comments: Option[List[ReactionView]], likes: Option[List[ReactionView]])

trait ReactionView {
  def id: String
  def authorName: String
  def authorId: String
  def reactionType: String
  def time: DateTime
  def fileName: Option[String]
  def lineNumber: Option[Int]
}

case class LikeView(id: String, authorName: String, authorId: String, time: DateTime, fileName: Option[String] = None, lineNumber: Option[Int] = None) extends ReactionView {
  val reactionType = "like"
}

case class CommentView(id: String, authorName: String, authorId: String, message: String, time: DateTime, authorAvatarUrl: String, fileName: Option[String] = None, lineNumber: Option[Int] = None) extends ReactionView {
  val reactionType = "comment"
}