package models

import play.api.libs.json._

case class Person(id: Long, name: String, age: Int)
case class Address(id: Long, personId: Long, address: String, city: String)

object Person {  
  implicit val personFormat = Json.format[Person]
}

object Address {
  implicit val personFormat = Json.format[Address]
}

