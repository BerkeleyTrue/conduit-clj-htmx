# Real World App

## The mother of all demo apps

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://github.com/raahii/golang-grpc-realworld-example/blob/master/LICENSE)

> ### Clojure/Hiccup/HTMX/XTDB codebase containing RealWorld examples (CRUD, auth, advanced patterns, etc) that adheres to the [RealWorld](https://github.com/gothinkster/realworld) spec as much as possible while sticking to HATEOAS principals.

### [Demo](https://github.com/gothinkster/realworld)&nbsp;&nbsp;&nbsp;&nbsp;[RealWorld](https://github.com/gothinkster/realworld)

This codebase was created to demonstrate a fully fledged backend application built with Clojure/HTMX including CRUD operations, authentication, routing, pagination, and more.

## How it works

- Using **Clojure** to implement realworld backend server.

  - Ring: [ring](https://github.com/ring-clojure/ring)
  - Reitit: [reitit]:(https://github.com/metosin/reitit)
  - Hiccup: [hiccup]:(https://github.com/weavejester/hiccup)
  - HTMX: [htmx](https://htmx.org/)
  - Hyperscript: [hyperscript](https://hyperscript.org/)

- Using **XTDB** to store data.
- Using **Nix Flakes** to build and deploy

## Getting started

The app listens and serves on `0.0.0.0:3000`.

Do drop into a nix shell (you will need the nix package manager installed)

```bash
nix develop
```

This will install the binaries needed as well as display useful information to get started developing.

## Project status

In Progress...

### Todo

- [ ] Docker Build
  > a build to let non-nix users run server

#### Doing

- [ ] switch to libsql

#### Done

- refactor to xtdb
  - [x] swap out datalevin for xtdb
  - [x] swap session to xtdb or some kv store other then datalevin
- Auth
  - [x] Add is authed middleware
  - [x] Return and list errors from form
  - [ ] Login
    - [x] Post to Login
    - [x] Add user to session
    - [x] Add user data to layout
    - [x] Flash message on success
  - [x] Sign Up
    - [x] Post
    - [x] Create user
    - [x] Flash message on success
    - [x] Sign out
    - [x] Can re-login
- Settings page
  - [x] Get settings page
  - [x] make errors dynamic
  - [x] add post for form
  - [x] add logout handler
  - [x] Update settings
    - [x] image
    - [x] username
    - [x] bio
    - [x] email
    - [x] password
- Get new/edit articles page
  - [x] edit tags
  - [x] /editor/slug - edit
    - [x] Load article and prefill page
    - [x] update links to patch
  - [x] /editor - new
- Home
  - [x] Render Page
  - [x] load global articles
  - [x] Load tags
  - [x] load user feed articles
  - [x] pagination
  - [x] load tags from article preview (FE only)
- Views
  - [x] add banners
  - [x] add error message on htmx 500
- Comments
  - API
    - [x] create comment on article
    - [x] delete comment from article
    - [x] get article comment by slug
  - Repo
    - [x] create
    - [x] read by id
    - [x] list by article id
    - [x] delete
  - Service
    - [x] create
    - [x] delete
    - [x] mark if owner
- Get Article page
  - [x] delete if owner
  - [x] Follow/Unfollow author
  - [x] Favorite/Unfavorite article
  - [x] comments
    - [x] Get on load
    - [x] mark if owner
    - [x] delete comment if owner
  - [x] /article/:slug
- Articles 
  - [x] Repo
    - [x] create
    - [x] get by id
    - [x] get by slug
    - [x] list by favorited
    - [x] update
    - [x] favorite
    - [x] unfavorite
    - [x] delete
  - Service
    - [x] create
    - [x] find
    - [x] update
    - [x] favorite
    - [x] unfavorite
    - [x] delete article
  - [x] Api
    - [x] Filter articles
      - [x] by author
      - [x] by tag
      - [x] by favorites
      - [x] by author
    - [x] Favorite
    - [x] Unfavorite
    - [x] Insert tags
    - [x] Get popular tags
    - [x] delete article
- Profile page
  - [x] Get profile page
  - [x] link to settings
  - [x] hide follow if own profile
  - [x] Make content live
  - [x] follow author
  - [ ] show favorited articles
- Add seed data import
  - [x] Add xtdb
  - [x] Add init script
  - [x] Add users
  - [x] Add articles
  - [x] Add default user on cli flag
  - [x] Add author details
  - [x] Add comments
