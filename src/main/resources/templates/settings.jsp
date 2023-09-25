<!doctype html>
<html lang="en">
<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <meta name="description" content="">
    <meta name="author" content="Mark Otto, Jacob Thornton, and Bootstrap contributors">
    <meta name="generator" content="Hugo 0.84.0">
    <title>🧙‍♂️ Arcana on Solana</title>

    <link rel="canonical" href="https://getbootstrap.com/docs/5.0/examples/navbar-fixed/">


    <!-- Bootstrap core CSS -->
    <link href="/static/bootstrap.min.css" rel="stylesheet">

    <style>
        .bd-placeholder-img {
            font-size: 1.125rem;
            text-anchor: middle;
            -webkit-user-select: none;
            -moz-user-select: none;
            user-select: none;
        }

        @media (min-width: 768px) {
            .bd-placeholder-img-lg {
                font-size: 3.5rem;
            }
        }
    </style>


    <!-- Custom styles for this template -->
    <link href="/static/navbar-top-fixed.css" rel="stylesheet">
    <script src="https://ajax.googleapis.com/ajax/libs/jquery/3.7.0/jquery.min.js"></script>
    <script src="/static/accounts.js"></script>
    <script>
        // push our LocalStorage our keys into the backend.
        $.post("/settings/localStorage", {localStorage: localStorage.getItem('arcanaAccounts')})
            .done(function (data) {
                //alert("Data Loaded: " + data);
            });
    </script>
    <script>
        // on page load, load private keys from back end and persist them.
        // this occurs on pkey add, otherwise is idempotent (no effect)
        $.get('/accounts/getAllPrivateAccounts', function (data, textStatus, jqXHR) {
            $.each(data, function (index, val) {
                if (!arcanaAccountsArray.includes(val.privatekey)) {
                    addArcanaAccount(val.privatekey);
                }
            });

            // TODO make this sync with the backend
            // alert(getLoadedArcanaAccounts());
        });

    </script>
</head>
<body>

<nav class="navbar navbar-expand-md navbar-dark fixed-top bg-dark">
    <div class="container-fluid">
        <a class="navbar-brand" href="#">🧙‍♂️ Arcana</a>
        <button class="navbar-toggler" type="button" data-bs-toggle="collapse" data-bs-target="#navbarCollapse"
                aria-controls="navbarCollapse" aria-expanded="false" aria-label="Toggle navigation">
            <span class="navbar-toggler-icon"></span>
        </button>
        <div class="collapse navbar-collapse" id="navbarCollapse">
            <ul class="navbar-nav me-auto mb-2 mb-md-0">
                <li class="nav-item">
                    <a class="nav-link" href="/">Home</a>
                </li>
                <li class="nav-item">
                    <a class="nav-link" href="/bots/add">✨ Strategies</a>
                </li>
                <li class="nav-item">
                    <a class="nav-link" href="/openbook">OpenBook</a>
                </li>
                <li class="nav-item">
                    <a class="nav-link active" aria-current="page" href="/settings">Settings</a>
                </li>
            </ul>
            <div class="d-flex navbar-text">
                RPC: <span th:text="${rpcEndpoint}" style="padding-left: 4px"></span>
            </div>
        </div>
    </div>
</nav>

<main class="container">
    <div class="bg-light p-5 rounded">
        <h2>Settings</h2>
        <div class="mb-3">
            <span class="input-group-text">Trading Accounts</span>
            <ul id="tradingAccounts">

            </ul>
            <script>
                // HTTP Get to load all accounts we have on the backend (show their public key)
                $.get('/accounts/getAllAccounts', function (data, textStatus, jqXHR) {
                    $.each(data, function (index, val) {
                        $("#tradingAccounts").append("<li>" +
                            "<a href=\"/bots/use/" + index + "\">" + val.pubkey + "</a>");
                    });
                });
            </script>
        </div>
        <div class="input-group mb-3">
            <form class="form-signin" method="POST" action="/settings">
                <div class="input-group-prepend">
                    <span class="input-group-text" id="rpc-server-text">RPC Server</span>
                </div>
                <label>
                    <input class="form-control" type="text" required aria-describedby="rpc-server-text" name="rpc">
                </label>
                <button class="btn btn-primary btn-block" type="submit">Save</button>
            </form>
        </div>
        <hr>
        <div class="input-group mb-3">
            Current account: <span th:text="${tradingAccountPubkey}"></span><br>
        </div>
        <hr>
        <div class="input-group mb-3">
            <form method="POST" action="/privateKeyUpload" enctype="multipart/form-data">
                Private Key (File): <input type="file" name="file"/><br/><br/>
                <input type="submit" class="btn btn-primary btn-block" value="Upload Private Key (File)"/>
            </form>
        </div>
        <hr>
        <div class="input-group mb-3">
            <form method="POST" action="/privateKeyPost">
                Private Key (Base58): <input type="text" name="privateKey"/><br/><br/>
                <input type="submit" class="btn btn-primary btn-block" value="Upload Private Key (Base58)"/>
            </form>
        </div>
    </div>
</main>


<script src="/static/bootstrap.bundle.min.js"></script>


</body>
</html>
