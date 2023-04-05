defmodule ContentProviders do
  use Plug.Router # import Plug.Router module. Functions for http method handling
  import Mogrify
  import MongoInterface
  require Logger

#PLUG PIPELINE
  plug Plug.Logger # debugging purposes
  plug :match # REQUIRED - main API to define endpoint routes
  #plug CORSPlug
  plug Plug.Parsers, parsers: [:multipart, :urlencoded, :json],length: 33_000_000, pass:  ["*/*"], json_decoder: Poison
  plug :dispatch # REQUIRED - dispatches responses after match and transformation

  def start_link do
    {:ok, [{ipv4,_,_},{_, _, _}]} = :inet.getif
    {:ok, _} = Plug.Adapters.Cowboy.http(ContentProviders, [ip: ipv4], port: 9000) #acceptors: 2
  end

  def init(options) do
    :ets.new(:entity_logos, [:set, :public, :named_table])
    {:ok, options}
  end

  get "/" do
    conn
        |> put_resp_content_type("text/html")
        |> send_file(200, "../BCassets/partners_web_access/front.html")
  end


  get "/login" do
      conn
        |> put_resp_content_type("text/html")
        |> send_file(200, "../BCassets/partners_web_access/login.html")
  end


  get "/register" do
      conn
        |> put_resp_content_type("text/html")
        |> send_file(200, "../BCassets/partners_web_access/register.html")
  end


  post "/partners/registration" do
    channel_name = conn.body_params["channel_name"]
    email = conn.body_params["email"]
    password = conn.body_params["password"]
    channel_logo = conn.body_params["channel_logo"]
    channel_desc = conn.body_params["channel_desc"]

    IO.inspect channel_name

    case channelExists(channel_name) do
      true -> send_resp(conn, 200, "0")
      false -> 
        File.mkdir!("../BCassets/channels/#{channel_name}")
        entity_logo_path = "../BCassets/channels/#{channel_name}/logo.png"
        File.cp(channel_logo.path, entity_logo_path)
        #reduce logo size with mogrify
        resizeForLogo(channel_name)

        #write logo base64 into ETS table :base64_logos
        {:ok, entity_logo_image_data} = File.read(entity_logo_path)
        entity_logo_base64 = Base.encode64(entity_logo_image_data)
        :ets.insert_new(:entity_logos, {channel_name, entity_logo_base64})

        createChannel(channel_name, email, password, channel_desc)


        token = Base.encode64(channel_name)
        [{_ets_entity_name, base64_partner_logo}] = :ets.lookup(:entity_logos, channel_name)
        {:ok, response } = 
        Map.new("token": token, 
                "url": "http://192.168.8.101:9000/index/logged/"<>token<>"/",
                "logo": base64_partner_logo)
        |> Poison.encode
        conn
          |> put_resp_content_type("text/html")
          |> send_resp(200, response)
    end
  end


  post "/attempt/login" do

    channel_name = conn.body_params["username"]

    case channelExists(channel_name) do
      true -> 
        token = Base.encode64(channel_name)
        [{_ets_entity_name, base64_partner_logo}] = :ets.lookup(:entity_logos, channel_name)
        #write to to database, then check when user arrives on /index that it matches.
        {:ok, response } = 
        Map.new("token": token, 
                "url": "http://192.168.8.101:9000/index/logged/"<> token <> "/",
                "logo": base64_partner_logo)
        #Map.new("token": token, "url": "http://192.168.8.101:9000/index/"<> token <> "/")
        |> Poison.encode
        conn
          |> put_resp_content_type("text/html")
          |> send_resp(200, response)
      false -> conn
                |> put_resp_content_type("text/html")
                |> send_resp(200, "0")
    end
  end


  match "/index/:route/:session_token/:username" do
    #ROUTES
    logged = "logged"
    aPost = "aPost"

    case isAuthentic(session_token, username) do
      true -> 
        case route do
          ^logged -> 
            conn
              |> put_resp_content_type("text/html")
              |> send_file(200, "../BCassets/partners_web_access/index.html")
          ^aPost ->
            newPost(conn)
            conn
              |> put_resp_content_type("text/html")
              |> send_resp(200, "post successful")
        end
      false ->
        conn
          |> put_resp_content_type("text/html")
          |> send_file(200, "../BCassets/partners_web_access/login.html")
    end
  end

  defp isAuthentic(session_token, username) do
    case Base.decode64(session_token) do
      {:ok, std} -> case username do
                      ^std -> true
                      _ -> false
                    end
      :error -> false
    end
  end

  defp newPost(conn) do
    channel_name = conn.body_params["channel_name"]
    article_cover = conn.body_params["cover"]

    article_title = conn.body_params["title"]
    article_media = conn.body_params["main_media"]
    article_body = conn.body_params["body"]

    IO.inspect channel_name

    IO.inspect article_cover.path
    IO.inspect article_title
    IO.inspect article_media
    IO.inspect article_body

    [{_ets_entity_name, base64_partner_logo}] = :ets.lookup(:entity_logos, channel_name)

    #COPY THE LSI TO ITS COLLECTION PATH
    File.cp(article_cover.path, "../BCassets/channels/#{channel_name}/lsi.png")

    main_content =
    if article_media != nil do
        case article_media.content_type do
          media_type when media_type == "image/jpeg" or media_type == "image/png" ->
            File.cp(article_media.path, "../BCassets/channels/#{channel_name}/image.png")
            resizeForFeed(channel_name)
            {:ok, imageData} = File.read("../BCassets/channels/#{channel_name}/image.png")
            "<img class='img' src='data:image/png;base64,"<>Base.encode64(imageData)<>"'/>"

          media_type when media_type == "video/mp4" -> 
            File.cp(article_media.path, "../BCassets/channels/#{channel_name}/video.mp4")
            #Thumbnex.create_thumbnail("../BCassets/channels/#{channel_name}/video.mp4", "../BCassets/channels/#{channel_name}/lsi.png", max_width: 500, max_height: 500)
            {:ok, videoData} = File.read("../BCassets/channels/#{channel_name}/video.mp4")
            "<video style='width: 100%;' controls poster=''><source src='data:video/webm;base64,"<>Base.encode64(videoData)<>"' type='video/mp4'></video>"

          media_type when media_type == "image/gif" ->   
            File.cp(article_media.path, "../BCassets/channels/#{channel_name}/gif.gif")
            #Thumbnex.create_thumbnail("../BCassets/channels/#{channel_name}/gif.gif", "../BCassets/channels/#{channel_name}/lsi.png", max_width: 500, max_height: 500)
            {:ok, gifData} = File.read("../BCassets/channels/#{channel_name}/gif.gif")
            "<img class='img' src='data:image/gif;base64,"<>Base.encode64(gifData)<>"'/>"
          media_type when media_type == "audio/mpeg" or media_type == "audio/mp3" ->
            File.cp(article_media.path, "../BCassets/channels/#{channel_name}/audio.mp3")
            #Thumbnex.create_thumbnail("../BCassets/channels/#{channel_name}/gif.gif", "../BCassets/channels/#{channel_name}/lsi.png", max_width: 500, max_height: 500)
            {:ok, audioData} = File.read("../BCassets/channels/#{channel_name}/audio.mp3")
            "<audio controls><source src='data:audio/mp3;base64,"<>Base.encode64(audioData)<>"' type='audio/mpeg'></audio>"
        end
      else
        ""
    end
    
    snippet = "<!DOCTYPE html><html><head><meta http-equiv='content-type' content='text/html; charset=UTF-8'> <title>BackChannel</title><style type='text/css'> body{margin: 0;} .marketing-header{margin: 8px;} #partner_logo{position:relative;width:20%;} .fixed-width{text-align:center} #homeSection{position: relative;width: 100%;} ul{position: relative; width: 100%; margin: 0; padding: 0;} ul > li {list-style: none;text-align: center;} .img{width:100%;} </style></head><body><div id='wrap'><header class='marketing-header'> <div class='fixed-width'> <img id='partner_logo' src='data:image/png;base64,"<>base64_partner_logo<>"' /><hr width='50%'><h2>"<>article_title<>"</h2> </div></header><div id='homeSection'><ul class='home-list'><li>"<>main_content<>"</li><li><p>"<>article_body<>"</p></li></ul></div> <!--end homeSection--></div><!--end wrapper--></body></html>"

    File.write("../BCassets/channels/#{channel_name}/content.html", snippet)

    partner_new_publish(channel_name)
  end





  get "/loadChannelsForBrowsing" do
    
    #get all logo base64 representations
    entity_logo_map = Map.new(:ets.tab2list(:entity_logos))
    conn 
    |> put_resp_content_type("text/html")
    |> send_resp(200, Poison.encode!(entity_logo_map))
  end



  match _ do
    conn
    |> send_resp(400, "bad request\r\n") 
    |> halt
  end

  defp resizeForLogo(channel_name) do
    Mogrify.open("../BCassets/channels/#{channel_name}/logo.png") 
    |> Mogrify.resize_to_limit("250x250")  #REDUCE THIS TO SMALLEST POSSIBLE
    |> Mogrify.save(in_place: true)
  end

  defp resizeForFeed(channel_name) do
    open("../BCassets/channels/#{channel_name}/image.png") 
    |> resize_to_limit("290x505")  #REDUCE THIS TO SMALLEST POSSIBLE
    |> save(in_place: true)
  end


  #FCM NOTIFICATION REQUESTS#
  defp partner_new_publish(partner) do
    HTTPoison.post("https://fcm.googleapis.com/fcm/send", 
      ~s({
                  "to": "/topics/#{ partner }",
                  "collapse_key":"collapse_all",
                  "data": {
                            "TYPE": "new_content",
                            "PARTNER": "#{ partner }"
                          },
                  "android": {
                            "priority": "high"
                          }
                }),
        [{"Content-Type", "application/json"}, 
         {"Authorization",  "key=AAAAI2UMlDU:APA91bHNgvbmPdB2BaIh8Sb4cyEpGZLQuUBI6o9oEMchu1uSXBXeZXcIQOKYY0ulAtet4VKs7ZBnV09ipDWbh4vJNZ4Tkwigx-NJDU5ftDAF2a9XiUe3TS1dZYxZLTX-AS6WsACKmdME"}
        ] 
      ) 
  end
  #FCM NOTIFICATION REQUESTS#

end