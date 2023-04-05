defmodule BackChannel.Mixfile do
  use Mix.Project

  def project do
    [
      app: :backchannel,
      version: "0.1.0",
      elixir: "~> 1.5",
      start_permanent: Mix.env == :prod,
      deps: deps()
    ]
  end

  # Run "mix help compile.app" to learn about applications.
  def application do
    [
      extra_applications: [:logger],
      mod: {BackChannel.Application, []}
    ]
  end

  # Run "mix help deps" to learn about dependencies.
  defp deps do
    [{:cowboy, "~> 1.0.3"},
    {:plug, "~> 1.0"},
    {:poison, "~> 3.1"},
    #{:httpotion, "~> 3.0.0"},
    #{:mogrify, "~> 0.6.1"},
    {:mogrify, "~> 0.5.4"},
    {:thumbnex, "~> 0.3.0"},
    {:mongodb, ">= 0.0.0"},
    {:poolboy, ">= 0.0.0"},
    {:httpoison, "~> 1.0"}]
    #{:cors_plug, "~> 1.5"}]
  end
end
