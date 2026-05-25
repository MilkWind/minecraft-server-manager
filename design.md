this is a minecraft server manager that provide multiple features to manage multiple servers.
architecture:
the frondend and backend are separated.
one frondend instance and one backend instance.
use path parameter to distinguish different servers and whether needs authentication routing to manager or without authentication routing to visitors. a url both include the server id and the client type.
so the access flow such as: for a visitor, he access the visitor url with specified server id, then view the server's information. for a manager, he access the manager url with specified server id and process 2FA authentication, then view the server's information.
use caddy handle the reverse proxy.
backend use SQLite as database, Mybatis Plus as ORM framework.
backend use Spring Boot as framework.
backend use spring-boot-starter-security as 2FA authentication implementation core.
specify a root directory of a server, then backend can control it.
frontend use Vue as framework.
use vue-router to handle the routing.
frontend ui component library use https://github.com/guokaigdg/animal-island-vue
frontend use [custom-directives](https://vuejs.org/guide/reusability/custom-directives.html) to hide the manager features from visitors.
code annotation use English but UI use Chinese.

for visitors, it supports features as follow:
view who is online
view what mods,datapacks,resourcepacks are loaded
view server's game version
view chat messages on the server(the chat messages include system logs so program need to filter out them from the system logs)
server performance monitor, includes CPU Usage, Memory Usage, Network Speed

for manager, it supports features as follow:
all features that visitors can use
access require 2FA authentication.
see all server's system logs and one-click copy feature.
start, stop, restart the server
op/deop players
ban players from the server
send manager messages to players(can send to all or specific players) on the server
suspend and resume specified mods,datapacks,resourcepacks, then restart the server
send custom commands(not hardcode in the code, it can be custom by manager and different custom command for different servers) to the server, there will have a specified ui area to create and use these custom commands.

