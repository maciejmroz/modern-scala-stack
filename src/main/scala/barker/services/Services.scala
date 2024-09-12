package barker.services

import cats.effect.IO
import doobie.Transactor

/** Just group all services in single place - enough for simple case
  */
final case class Services(barkService: BarkService, userService: UserService)

object Services:
  def apply(): IO[Services] =
    for
      user <- UserService()
      bark <- BarkService()
    yield Services(bark, user)

  def apply(xa: Transactor[IO]): IO[Services] =
    for
      user <- UserService(xa)
      bark <- BarkService() // TODO
    yield Services(bark, user)
    
