package usafe.digital.verifiablecredentials

import cats.effect.{ConcurrentEffect, ExitCode, IO, IOApp, Sync, Timer}
import cats.syntax.all._
import fs2.Stream
import izumi.logstage.api.IzLogger
import izumi.logstage.api.routing.StaticLogRouter
import izumi.logstage.sink.ConsoleSink
import logstage.LogIO
import org.http4s.HttpRoutes
import org.http4s.syntax.kleisli._
import org.http4s.client.Client
import org.http4s.client.blaze.BlazeClientBuilder
import org.http4s.server.Router
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.client.middleware.{Logger => ClientLogger}
import org.http4s.server.middleware.{Logger => ServerLogger}
import pureconfig.module.catseffect._
import pureconfig.generic.auto._
import usafe.digital.verifiablecredentials.config.types.AppConfig
import usafe.digital.verifiablecredentials.config.implicits._

import scala.concurrent.duration._
import scala.concurrent.ExecutionContext

object main extends IOApp {
  override def run(args: List[String]): IO[ExitCode] = server[IO].compile.lastOrError

  private def server[F[_]: ConcurrentEffect: Sync: Timer]: Stream[F, ExitCode] = for {
    client <- BlazeClientBuilder[F](ExecutionContext.global).withConnectTimeout(5.seconds).stream

    cfg <- Stream.eval(loadConfigF[F, AppConfig])

    stdLogger = IzLogger(IzLogger.Level.Info, ConsoleSink.text(true))
    fnLogger = LogIO.fromLogger[F](stdLogger)
    _ = StaticLogRouter.instance.setup(stdLogger.router)

    loggedClient = ClientLogger(
      logHeaders = true,
      logBody = true,
      logAction = debugLogFn(fnLogger)
    )(client)

    routes <- Stream.eval(httpProgram(loggedClient, cfg))

    loggedRoutes = ServerLogger.httpRoutes(
      logHeaders = true,
      logBody = true,
      logAction = debugLogFn(fnLogger)
    )(routes)

    server <- BlazeServerBuilder[F].withHttpApp(
      Router(
        "/" -> loggedRoutes
      ).orNotFound
    ).bindHttp(cfg.httpPort.value)
        .serve

  } yield server


  private def httpProgram[F[_]: Sync](client: Client[F], cfg: AppConfig): F[HttpRoutes[F]] = for {
    ks <- usafe.digital.proof.ops.loadKeys[F]
    (priv, _) = ks
    http = endpoint.http.postVerifiableCredentialsRequest(cfg, priv, client)
  } yield http

  private def debugLogFn[F[_]: Sync](logger: LogIO[F]): Option[String => F[Unit]] = { s: String =>
    logger.info(s"$s")
    }.some


}
