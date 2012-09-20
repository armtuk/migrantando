import anorm._
import scala.Some

import play.api.db._
import play.api.Play.current

import anorm.SqlParser._

/**
 * Created with IntelliJ IDEA.
 * User: aturner
 * Date: 6/12/12
 * Time: 12:29 PM
 */

trait IdDriven {
  val id: Option[Long]
}

trait IdDriveCompanion[T <: IdDriven] {
  val tableName: String

  def persist(t: T): T

  def update(t: T): T

  val simple: RowParser[T]

  def saveOrUpdate(e: T): T = {
    e.id match {
      // Check if the object has an id
      case Some(x) => {
        // If it does have one then....
        read(x) match {
          // Then check if it exists in the database (and return it if so, which is a waste)
          case Some(t) => update(e) // If it does exist - update the database copy
          case None => persist(e) // Otherwise save it for the first time
        }
      }
      case None => persist(e) // If there is no id in our object, then assume it's new, and save/create it in the DB.
    }
  }

  def read(id: Long): Option[T] = {
    DB.withConnection {
      implicit connection =>
        SQL("select * from solar_system where solar_system_id = {id}")
          .on('id -> id).as(simple.singleOpt)
    }
  }

  def list() = {
    DB.withConnection {
      implicit connection =>
        SQL("select * from " + tableName).as(simple *)
    }
  }
}
