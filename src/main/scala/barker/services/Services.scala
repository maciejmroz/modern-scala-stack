package barker.services

import cats.effect.IO

/** Just group all services in single place - enough for simple case
  */
final case class Services(barkService: BarkService, userService: UserService)

object Services:
  def apply(): IO[Services] =
    for
      user <- UserService()
      bark <- BarkService()
    yield Services(bark, user)
