defmodule BackChannel.Application do
  # See https://hexdocs.pm/elixir/Application.html
  # for more information on OTP Applications
  @moduledoc false

  use Application

  def start(_type, _args) do
    #import Supervisor.Spec, warn: false
    # List all child processes to be supervised
    children = [
         #%{
         #   id: Mongo,
         #   start: {Mongo, :start_link, [[name: :mongo, database: "BackChannel", pool: DBConnection.Poolboy]]}
         # },
          %{
            id: MongoInterface,
            start: {MongoInterface, :start_link, []}
          },
          %{
            id: BackChannel,
            start: {BackChannel, :start_link, []}
          },
          %{
            id: Public_interface,
            start: {Public_interface, :start_link, []}
          },
          %{
            id: ContentProviders,
            start: {ContentProviders, :start_link, []}
          },
          %{
            id: AutoContentGen,
            start: {AutoContentGen, :start_link, []}
          }

    ]

    # See https://hexdocs.pm/elixir/Supervisor.html
    # for other strategies and supported options
    opts = [strategy: :one_for_one, name: BackChannel.Supervisor]
    Supervisor.start_link(children, opts)
  end
end
