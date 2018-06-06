package models

import javax.inject.{ Inject, Singleton }
import play.api.db.slick.DatabaseConfigProvider
import slick.jdbc.JdbcProfile

import scala.concurrent.{ Future, ExecutionContext }

/**
 * A repository for people.
 *
 * @param dbConfigProvider The Play db config provider. Play will inject this for you.
 */
@Singleton
class PersonRepository @Inject() (dbConfigProvider: DatabaseConfigProvider)(implicit ec: ExecutionContext) {
  // We want the JdbcProfile for this provider
  private val dbConfig = dbConfigProvider.get[JdbcProfile]

  // These imports are important, the first one brings db into scope, which will let you do the actual db operations.
  // The second one brings the Slick DSL into scope, which lets you define the table and other queries.
  import dbConfig._
  import profile.api._

  /**
   * Here we define the table. It will have a name of people
   */
  private class PeopleTable(tag: Tag) extends Table[Person](tag, "people") {

    /** The ID column, which is the primary key, and auto incremented */
    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)

    /** The name column */
    def name = column[String]("name")

    /** The age column */
    def age = column[Int]("age")

    /**
     * This is the tables default "projection".
     *
     * It defines how the columns are converted to and from the Person object.
     *
     * In this case, we are simply passing the id, name and page parameters to the Person case classes
     * apply and unapply methods.
     */
    def * = (id, name, age) <> ((Person.apply _).tupled, Person.unapply)
  }

  private class AddressTable(tag: Tag) extends Table[Address](tag, "address") {

    /** The ID column, which is the primary key, and auto incremented */
    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)

    def personId = column[Long]("personId")

    /** The address column */
    def address = column[String]("address")

    /** The city column */
    def city = column[String]("city")

    /** The the foreign key column */
    def user = foreignKey("user_fk", personId, people)(_.id)

    def * = (id, personId, address, city) <> ((Address.apply _).tupled, Address.unapply)
  }

  private val addresses =  TableQuery[AddressTable]


  /**
   * The starting point for all queries on the people table.
   */
  private val people = TableQuery[PeopleTable]

  /**
   * Create a person with the given name and age.
   *
   * This is an asynchronous operation, it will return a future of the created person, which can be used to obtain the
   * id for that person.
   */
  def create(name: String, age: Int): Future[Person] = db.run {
    // We create a projection of just the name and age columns, since we're not inserting a value for the id column
    (people.map(p => (p.name, p.age))
      // Now define it to return the id, because we want to know what id was generated for the person
      returning people.map(_.id)
      // And we define a transformation for the returned value, which combines our original parameters with the
      // returned id
      into ((nameAge, id) => Person(id, nameAge._1, nameAge._2))
    // And finally, insert the person into the database
    ) += (name, age)
  }

  /**
   * List all the people in the database.
   */
  def list(): Future[Seq[Person]] = db.run {
    people.result
  }

  def insertAddress (personId: Int, address: String, city: String): Future[Int] = db.run {
    addresses.map(p => (p.personId, p.address, p.city )) +=(personId, address, city)
  }

  def insertPerson (name: String, age: Int): Future[Int]= db.run {
    people.map(p => (p.name,p.age)) +=(name, age)
  }

  def listAddress(): Future[Seq[Address]] = db.run{
    addresses.result
  }

  //This is an innerJoin
  def innerJoin() : Future[Seq[(Person,Address)]] = db.run{
    people.
      join(addresses).on(_.id === _.personId).result
  }

  //This is the query for leftJoin
  def leftJoin (): Future[Seq[(Person,Option[Address])]] = db.run {
    people.joinLeft(addresses).on(_.id === _.personId).result
  }

  //This query is for rightJoin
  def rightJoin (): Future[Seq[(Option[Person],Address)]] = db.run{
    people.joinRight(addresses).on(_.id === _.personId).result
  }

  //this query is for full join
  def fullJoin (): Future[Seq[(Option[Person],Option[Address])]] = db.run {
    people.joinFull(addresses).on(_.id === _.personId).result
  }


}
