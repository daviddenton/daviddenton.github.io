import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.Result
import dev.forkhandles.result4k.Success
import io.mockk.every
import io.mockk.mockk
import org.http4k.client.OkHttp
import org.http4k.core.Filter
import org.http4k.core.HttpHandler
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Uri
import org.http4k.core.then
import org.http4k.filter.ClientFilters.SetBaseUriFrom
import org.http4k.filter.ServerFilters.BearerAuth
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.http4k.server.Netty
import org.http4k.server.asServer
import org.junit.jupiter.api.Test

object before {
    fun MySecureApp(): HttpHandler =
        BearerAuth("my-very-secure-and-secret-bearer-token")
            .then(
                routes(
                    echo(),
                    health()
                )
            )

    fun echo() = "/echo" bind POST to { req: Request -> Response(OK).body(req.bodyString()) }

    fun health() = "/health" bind GET to { req: Request -> Response(OK).body("alive!") }

    val server = MySecureApp().asServer(Netty(8080)).start()

    class GitHubApi(client: HttpHandler) {
        private val http = SetBaseUriFrom(Uri.of("https://api.github.com"))
            .then(SetHeader("Accept", "application/vnd.github.v3+json"))
            .then(client)

        fun getUser(username: String): UserDetails {
            val response = http(Request(GET, "/users/$username"))
            return UserDetails(userNameFrom(response), userOrgsFrom(response))
        }

        fun getRepoLatestCommit(owner: String, repo: String) = Commit(
            authorFrom(http(
                Request(GET, "/repos/$owner/$repo/commits").query("per_page", "1")
            ))
        )
    }

    val gitHub: GitHubApi = GitHubApi(OkHttp())
    val user: UserDetails = gitHub.getUser("octocat")

}

@Test
fun `translates request`() {
    assertThat(GetUser("foobar").toRequest(), equalTo(Request(GET, "/users/foobar")))
}

@Test
fun `translates response`() {
    assertThat(GetUser("foobar").fromResponse(Response(OK).body("foobar/admin,mgmt")),
        equalTo(UserDetails("foobar", listOf("admin", "mgmt"))))
}

fun authorFrom(response: Response) = "bob"
fun userNameFrom(response: Response) = "bob"
fun userOrgsFrom(response: Response) = listOf<String>()

// interface
interface GitHubApiAction<R> {
    fun toRequest(): Request
    fun fromResponse(response: Response): R
}

// action/response
data class GetUser(val username: String) : GitHubApiAction<UserDetails> {
    override fun toRequest() = Request(GET, "/users/$username")
    override fun fromResponse(response: Response) = UserDetails(userNameFrom(response), userOrgsFrom(response))
}
data class UserDetails(val name: String, val orgs: List<String>)

data class GetRepoLatestCommit(val owner: String, val repo: String) : GitHubApiAction<Commit> {
    override fun toRequest() = Request(GET, "/repos/$owner/$repo/commits").query("per_page", "1")
    override fun fromResponse(response: Response) = Commit(authorFrom(response))
}
data class Commit(val author: String)


interface GitHubApi {
    operator fun <R : Any> invoke(action: GitHubApiAction<R>): R

    companion object
}

// adapter
fun GitHubApi.Companion.Http(client: HttpHandler) = object : GitHubApi {
    private val http = SetBaseUriFrom(Uri.of("https://api.github.com"))
        .then(SetHeader("Accept", "application/vnd.github.v3+json"))
        .then(client)

    override fun <R : Any> invoke(action: GitHubApiAction<R>) = action.fromResponse(http(action.toRequest()))
}

val gitHub: GitHubApi = GitHubApi.Http(OkHttp())
val user: UserDetails = gitHub.getUser("octocat")

// extension function - nicer API
fun GitHubApi.getUser(username: String) = invoke(GetUser(username))
fun GitHubApi.getLatestRepoCommit(owner: String, repo: String): Commit = invoke(GetRepoLatestCommit(owner, repo))

fun GitHubApi.getLatestUser(org: String, repo: String): UserDetails {
    val commit = getLatestRepoCommit(org, repo)
    return getUser(commit.author)
}

val latestUser: UserDetails = gitHub.getLatestUser("http4k", "http4k-connect")

@Test
fun `get user details`() {
    val githubApi = mockk<GitHubApi>()
    val userDetails = UserDetails("bob", listOf("http4k"))
    every { githubApi(any<GetUser>()) } returns userDetails

    assertThat(githubApi.getUser("bob"), equalTo(userDetails))
}

class RecordingGitHubApi(private val delegate: GitHubApi) : GitHubApi {
    val recorded = mutableListOf<GitHubApiAction<*>>()
    override fun <R : Any> invoke(action: GitHubApiAction<R>): R {
        recorded += action
        return delegate(action)
    }
}

class StubGitHubApi(private val users: Map<String, UserDetails>) : GitHubApi {
    override fun <R : Any> invoke(action: GitHubApiAction<R>): R = when (action) {
        is GetUser -> getUser(action, users) as R
        is GetRepoLatestCommit -> getRepoLatestCommit(action) as R
        else -> throw UnsupportedOperationException()
    }
}

private fun getUser(action: GetUser, users: Map<String, UserDetails>) = users[action.username]
private fun getRepoLatestCommit(action: GetRepoLatestCommit) = Commit(action.owner)


fun SetHeader(name: String, value: String): Filter = TODO()

object result4k {
    interface GitHubApiAction<R> {
        fun toRequest(): Request
        fun fromResponse(response: Response): Result<R, Exception>
    }

    data class GetUser(val username: String) : GitHubApiAction<UserDetails> {
        override fun toRequest() = Request(GET, "/users/$username")
        override fun fromResponse(response: Response) = when {
            response.status.successful -> Success(UserDetails(userNameFrom(response), userOrgsFrom(response)))
            else -> Failure(RuntimeException("API returned: " + response.status))
        }
    }
}
