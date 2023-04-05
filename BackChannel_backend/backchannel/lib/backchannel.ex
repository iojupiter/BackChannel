defmodule BackChannel do
  use Plug.Router # import Plug.Router module. Functions for http method handling
  import Mogrify
  require Logger
  import MongoInterface

#PLUG PIPELINE
  plug Plug.Logger # debugging purposes
  plug :match # REQUIRED - main API to define endpoint routes
  plug Plug.Parsers, parsers: [:multipart, :urlencoded], pass:  ["*/*"]
  plug :dispatch # REQUIRED - dispatches responses after match and transformation

  def start_link do
    {:ok, [{ipv4,_,_},{_, _, _}]} = :inet.getif
    {:ok, _} = Plug.Adapters.Cowboy.http(BackChannel, [ip: ipv4], port: 7777) #acceptors: 2
    #{:ok, _} = Plug.Adapters.Cowboy.http(Client_interface, port: 7777)
  end

  def init(options), do: options

    #0 and 1 responses to front end are daft
    #what is the point of copying snippet.html to root user folder?

  get "/user/:username_hash/:token" do
    case userExists(username_hash) do
      true -> 
        updateToken(username_hash, token)
        send_resp(conn, 200, "1")
      false -> send_resp(conn, 200, "0")
    end
  end


  get "/registration/:username/:token/:screen" do
    username_hash = Base.encode16(username, case: :lower)
    case userExists(username_hash) do
      false -> 
        createUser(username, username_hash, token, screen)
        case File.mkdir("../BCassets/#{username_hash}") do
          :ok -> 
            File.cp("../BCassets/templates/snippet.html", "../BCassets/#{username_hash}/snippet.html")
            send_resp(conn, 200, username_hash)
          {:error, :eexist} -> 
            IO.inspect("FILE #{username_hash} ALREADY EXISTS")
            send_resp(conn, 200, username_hash)
        end
      true -> send_resp(conn, 200, "taken" )
    end
  end


  get "/searchFriend/:username_hash/*variable" do
    case IO.inspect("#{variable}") do
      "" -> conn |> send_resp(400, "bad request\r\n") |> halt
      _ ->  case userExists(username_hash) do
              true -> 
                conn
                |> put_resp_content_type("application/json")
                |> send_resp(200, Poison.encode!( searchUserbase("#{variable}") ))
              false -> conn |> send_resp(400, "bad request\r\n") |> halt
            end
    end
  end


  get "/sendFriendRequest/:username_hash/:friend" do
    friend_hash = Base.encode16(friend, case: :lower)
    {:ok, username} = Base.decode16(username_hash, case: :lower)
    case userExists(username_hash) do
      true -> case userExists(friend_hash) do
                true -> case addFriendRequest(username, friend) do
                          true -> 
                            sendFriendRequestNotification(getToken(friend))
                            send_resp(conn, 200, friend)
                          false -> send_resp(conn, 200, friend)
                        end
                false -> conn |> send_resp(400, "bad request\r\n") |> halt
              end
      false -> conn |> send_resp(400, "bad request\r\n") |> halt
    end
  end


  get "/deleteFriend/:username_hash/:friend" do
    friend_hash = Base.encode16(friend, case: :lower)
    {:ok, username} = Base.decode16(username_hash, case: :lower)
    case userExists(username_hash) do
      true -> case userExists(friend_hash) do
                true -> 
                  deleteFriend(username, friend)
                  send_resp(conn, 200, "Friend deleted")
                false -> conn |> send_resp(400, "bad request\r\n") |> halt
              end
      false -> conn |> send_resp(400, "bad request\r\n") |> halt
    end
  end


  get "/sync/:username_hash" do
    {:ok, username} = Base.decode16(username_hash, case: :lower)
    case userExists(username_hash) do
      true -> 
        conn
        |> put_resp_content_type("application/json")
        |> send_resp(200, Poison.encode!( checkForUpdates(username) ))
      false -> conn |> send_resp(400, "bad request\r\n") |> halt
    end
  end


  get "/updateProfile/:username_hash/:profile" do
    {:ok, username} = Base.decode16(username_hash, case: :lower) 
    case userExists(username_hash) do
      true -> 
        updateFriendsList(username, profile)
        send_resp(conn, 200, "Saved")
      false -> conn |> send_resp(400, "bad request\r\n") |> halt
    end
  end


  post "/backchannel/:username_hash/:target" do
    {:ok, username} = Base.decode16(username_hash, case: :lower) 
    target_hash = Base.encode16(target, case: :lower)
    case userExists(username_hash) do
      true -> case userExists(target_hash) do
                true -> 
                  File.cp(conn.body_params["image"].path, "../BCassets/#{target_hash}/lsi.png")
                  resizeForLockscreen(target_hash)
                  resizeForFeed(target_hash)
                  {:ok, imageData} = File.read("../BCassets/#{target_hash}/img_for_feed.png")
                  base64data = Base.encode64(imageData)
                  File.write("../BCassets/#{target_hash}/snippet.html", "<!--NEW--><div><img class='image' src='data:image/jpg;base64,"<>base64data<>"'/><p class='message'>"<>conn.body_params["Message"]<>"</p><p class='friend'>- "<>username<>"</p><p class='time'>"<>conn.body_params["Time"]<>"</p><hr width='70%'></div>")
                  pushTech(getToken(target))
                  send_resp(conn, 200, "Successful")
                false -> conn |> send_resp(400, "bad request\r\n") |> halt
              end
      false -> conn |> send_resp(400, "bad request\r\n") |> halt
    end
  end


  get "/friendRequests/:username_hash" do
    {:ok, username} = Base.decode16(username_hash, case: :lower)
    case userExists(username_hash) do
      true -> send_resp(conn, 200, Poison.encode!( getFriendRequests(username) ))
      false -> conn |> send_resp(400, "bad request\r\n") |> halt
    end
  end

  get "/removeInFRL/:username_hash/:friend" do
    target_hash = Base.encode16(friend, case: :lower)
    {:ok, username} = Base.decode16(username_hash, case: :lower)
    case userExists(username_hash) do
      true -> case userExists(target_hash) do
                true -> case removeInFRL(username, friend) do
                          true -> send_resp(conn, 200, "Friend deleted")
                          false -> send_resp(conn, 200, "Friend not deleted")
                        end
                false -> conn |> send_resp(400, "bad request\r\n") |> halt
              end
      false -> conn |> send_resp(400, "bad request\r\n") |> halt
    end
  end




  match _ do
    conn
    |> send_resp(400, "bad request\r\n") 
    |> halt
  end

  defp resizeForFeed(target_hash) do
    open("../BCassets/#{target_hash}/lsi.png") 
    |> resize_to_limit("300x300")  #REDUCE THIS TO SMALLEST POSSIBLE
    |> save(path: "../BCassets/#{target_hash}/img_for_feed.png")
  end

  defp resizeForLockscreen(target_hash) do
    open("../BCassets/#{target_hash}/lsi.png") 
    |> resize_to_limit("200x200")
    #|> resize_to_limit(MongoInterface.getScreenSize(target_hash))
    |> save(in_place: true)
  end

  #FCM NOTIFICATION REQUESTS#
  def pushTech(token) do
    IO.inspect "PRIVATE NOTIFICATION sent to FCM"
    HTTPoison.post("https://fcm.googleapis.com/fcm/send", 
      ~s({
                  "to":"#{ token }",
                  "collapse_key":"collapse_all",
                  "data": { 
                    "TYPE": "private_bc"
                          },
                  "android":{
                        "priority":"high"
                            }
       }),
        [{"Content-Type", "application/json"}, 
         {"Authorization",  "key=AAAAI2UMlDU:APA91bHNgvbmPdB2BaIh8Sb4cyEpGZLQuUBI6o9oEMchu1uSXBXeZXcIQOKYY0ulAtet4VKs7ZBnV09ipDWbh4vJNZ4Tkwigx-NJDU5ftDAF2a9XiUe3TS1dZYxZLTX-AS6WsACKmdME"}
        ])
  end

  def sendFriendRequestNotification(token) do
    IO.inspect "PRIVATE FRIEND REQ NOTIFICATION sent to FCM"
    HTTPoison.post("https://fcm.googleapis.com/fcm/send", 
      ~s({
                  "to" : "#{ token }",
                  "collapse_key" : "collapse_all",
                  "data": { 
                    "TYPE": "notification"
                          },
                  "notification" : {
                  "title": "You have a new friend request"
                                    }
                }),
        [{"Content-Type", "application/json"}, 
         {"Authorization",  "key=AAAAI2UMlDU:APA91bHNgvbmPdB2BaIh8Sb4cyEpGZLQuUBI6o9oEMchu1uSXBXeZXcIQOKYY0ulAtet4VKs7ZBnV09ipDWbh4vJNZ4Tkwigx-NJDU5ftDAF2a9XiUe3TS1dZYxZLTX-AS6WsACKmdME"}
        ]) 
  end
  #FCM NOTIFICATION REQUESTS#

end