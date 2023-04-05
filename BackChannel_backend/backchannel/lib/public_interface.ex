defmodule Public_interface do
	use Plug.Router # import Plug.Router module. Functions for http method handling
	import MongoInterface
	require Logger

#PLUG PIPELINE
	plug Plug.Logger # debugging purposes
	plug :match # REQUIRED - main API to define endpoint routes
	plug Plug.Parsers, parsers: [:multipart, :urlencoded], pass:  ["*/*"]
	plug :dispatch # REQUIRED - dispatches responses after match and transformation

	def start_link do
		{:ok, [{ipv4,_,_},{_, _, _}]} = :inet.getif
		{:ok, _} = Plug.Adapters.Cowboy.http(Public_interface, [ip: ipv4], port: 8000) #acceptors: 2
	end

	def init(options), do: options


	get "/private/:username_hash" do
		IO.inspect conn
		case userExists(username_hash) do
			true -> 
				{:ok, snippet} = File.read("../BCassets/#{username_hash}/snippet.html")
				conn
				|> put_resp_content_type("image/png")
				|> put_resp_header("snippet", snippet)
				|> send_file(200, "../BCassets/#{username_hash}/lsi.png")
			false -> conn |> send_resp(400, "bad request\r\n") |> halt
		end
	end

	get "/public/:content_provider/:username_hash" do
		case userExists(username_hash) do
			true -> 
				conn
				|> put_resp_content_type("image/png")
				|> put_resp_header("url","http://192.168.8.101:8000/partner/#{content_provider}")
				|> send_file(200, "../BCassets/channels/#{content_provider}/lsi.png")
			false -> conn |> send_resp(400, "bad request\r\n") |> halt
		end
	end

    get "/partner/:provider" do
    	conn
    	|> put_resp_content_type("text/html")
    	|> send_file(200, "../BCassets/channels/#{provider}/content.html")
    end



	match _ do
		conn
		|> send_resp(400, "bad request\r\n") 
		|> halt
	end

end