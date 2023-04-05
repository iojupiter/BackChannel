defmodule AutoContentGen do
  use GenServer

  def start_link do
    GenServer.start_link(__MODULE__, :ok, [])
  end

  def init(state) do
    {:ok, state}
  end

  def giphy do
    IO.puts "FETCHING NEW GIF CONTENT"
    response = HTTPoison.get("http://api.giphy.com/v1/gifs/trending?api_key=dc6zaTOxFJmzC&limit=1", 
      [body: ~s(),
        headers: [] 
      ])
    %{"data" => [great_map]} = Poison.Parser.parse!(response.body)
    gifResponse =
    Map.get(great_map, "images")
    |> Map.get("downsized")
    |> Map.get("url")
    |> HTTPoison.get

    [{_ets_entity_name, base64_partner_logo}] = :ets.lookup(:entity_logos, "giphy")
    
    File.write("../BCassets/channels/giphy/gif.gif", gifResponse.body)

    Thumbnex.create_thumbnail("../BCassets/channels/giphy/gif.gif", "../BCassets/channels/giphy/lsi.png", max_width: 500, max_height: 500)
    {:ok, gifData} = File.read("../BCassets/channels/giphy/gif.gif")
    main_content = "<img class='img' src='data:image/gif;base64,"<>Base.encode64(gifData)<>"'/>"
    snippet = "<!DOCTYPE html><html><head><meta http-equiv='content-type' content='text/html; charset=UTF-8'> <title>BackChannel</title><style type='text/css'> body{margin: 0;} .marketing-header{margin: 8px;} #partner_logo{position:relative;width:20%;} .fixed-width{text-align:center} #homeSection{position: relative;width: 100%;} ul{position: relative; width: 100%; margin: 0; padding: 0;} ul > li {list-style: none;text-align: center;} .img{width:100%;} </style></head><body><div id='wrap'><header class='marketing-header'> <div class='fixed-width'> <img id='partner_logo' src='data:image/png;base64,"<>base64_partner_logo<>"' /><hr width='50%'><h2>Trending GIFs</h2> </div></header><div id='homeSection'><ul class='home-list'><li>"<>main_content<>"</li><li><a href='#'>Powered by GIPHY</a></li></ul></div> <!--end homeSection--></div><!--end wrapper--></body></html>"

    File.write("../BCassets/channels/giphy/content.html", snippet)
    partner_new_publish("giphy")
    
    #:timer.sleep(:timer.minutes(20))
    #AutoContentGen.giphy
  end

  def random do
    response = HTTPoison.get("https://api.unsplash.com/photos/random?client_id=0515ded9fa834ae610deb2b7d5cd63247a45ab3f0b56d3332fa9f9b512f84cd5",
      [body: ~s(
              "w": "200",
              "h": "600"
              ),
        headers: [] 
      ])
    imgResponse =
    Poison.Parser.parse!(response.body)
    |> Map.get("urls")
    |> Map.get("regular")
    |> HTTPoison.get

    [{_ets_entity_name, base64_partner_logo}] = :ets.lookup(:entity_logos, "random")

    article_body =
    Poison.Parser.parse!(response.body)
    |> Map.get("description")

    IO.inspect article_body

    File.write("../BCassets/channels/random/lsi.png", imgResponse.body)
    {:ok, imageData} = File.read("../BCassets/channels/random/lsi.png")
    main_content = "<img class='img' src='data:image/png;base64,"<>Base.encode64(imageData)<>"'/>"

    #snippet = "<!DOCTYPE html><html><head><meta http-equiv='content-type' content='text/html; charset=UTF-8'> <title>BackChannel</title><style type='text/css'> body{margin: 0;} .marketing-header{margin: 8px;} #partner_logo{position:relative;width:20%;} .fixed-width{text-align:center} #homeSection{position: relative;width: 100%;} ul{position: relative; width: 100%; margin: 0; padding: 0;} ul > li {list-style: none;text-align: center;} .img{width:100%;} </style></head><body><div id='wrap'><header class='marketing-header'> <div class='fixed-width'> <img id='partner_logo' src='data:image/png;base64,"<>base64_partner_logo<>"' /><hr width='50%'><h2></h2> </div></header><div id='homeSection'><ul class='home-list'><li>"<>main_content<>"</li><li><p>"<>article_body<>"</p></li><li><a href='#'>courtesy Unsplash</a></li></ul></div> <!--end homeSection--></div><!--end wrapper--></body></html>"
    snippet = "<!DOCTYPE html><html><head><meta http-equiv='content-type' content='text/html; charset=UTF-8'> <title>BackChannel</title><style type='text/css'> body{margin: 0;} .marketing-header{margin: 8px;} #partner_logo{position:relative;width:20%;} .fixed-width{text-align:center} #homeSection{position: relative;width: 100%;} ul{position: relative; width: 100%; margin: 0; padding: 0;} ul > li {list-style: none;text-align: center;} .img{width:100%;} </style></head><body><div id='wrap'><header class='marketing-header'> <div class='fixed-width'> <img id='partner_logo' src='data:image/png;base64,"<>base64_partner_logo<>"' /><hr width='50%'><h2></h2> </div></header><div id='homeSection'><ul class='home-list'><li>"<>main_content<>"</li><li><a href='#'>courtesy Unsplash</a></li></ul></div> <!--end homeSection--></div><!--end wrapper--></body></html>"

    File.write("../BCassets/channels/random/content.html", snippet)
    partner_new_publish("random")
    
    :timer.sleep(:timer.minutes(30))
    AutoContentGen.random
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
        ])
  end
  #FCM NOTIFICATION REQUESTS#
end